import os
from argparse import ArgumentParser
import simplejson as json

from src.elements.node_list import NodeList
from src.parser.osm_parser import OSMParser
from src.parser.gtfs_parser import GTFSParser
from src.schedule_converter import ScheduleConverter
from src.util.store_key_pair import StoreKeyPair
from src.osm_extender import OSMExtender
from src.gps2wkt import GPS2WKT


class PublicTransportConverter:
    def __init__(self):
        parser = ArgumentParser(description='Extend osm xml data by gtfs stations and connect them to their stop points')
        parser.add_argument('-osm', '--osm', required=True, help="Input OSM XML file")
        parser.add_argument('-gtfs', '--gtfs', required=True, help='Input GTFS sqlite database (see module pygtfs)')
        parser.add_argument('-f', '--filter', action=StoreKeyPair, nargs="?",
                            default=[('railway', 'stop'), ('railway', 'halt'), ('railway', 'station'), ('public_transport', 'stop_position')],
                            help="Add filter to search for nodes. Use list in form of KEY1=VAL1,KEY2=VAL2. Each entry is connected with logical or.")
        parser.add_argument('-d', '--distance', nargs='?', type=int, default=1000, help="Maximum distance between gtfs station and osm stop positions.")
        parser.add_argument('-o', '--output', nargs='?', default='', help="Output directory. The directory of the source OSM File is used by default.")
        parser.add_argument('-s', '--no-scale', action='store_true', default=False, help="Disable scaling of the map for simulation.")
        parser.add_argument('-x', '--max_x', nargs="?", default='4500', type=int, help="Maximum size of x dimension within simulation.")
        parser.add_argument('-y', '--max_y', nargs="?", default='3400', type=int, help="Maximum size of y dimension within simulation.")
        parser.add_argument('-b', '--begin', nargs="?", default='',
                            help="Set the date where the simulation should begin in the gtfs schedule. %%d.%%m.%%Y (e.g. 1.1.2018) "
                                 "Defaults to the first monday in the schedule. "
                                 "If set --end has to be set as well.")
        parser.add_argument('-e', '--end', nargs="?", default='',
                            help="Set the date where the simulation should end in thegtfs schedule. %%d.%%m.%%Y (e.g. 31.1.2018) "
                                 "Defaults to the first sunday after the first monday. "
                                 "If set --begin has to be set as well.")
        self.args = parser.parse_args()

        if not self.args.output:
            self.output = self.args.osm
        else:
            self.output = os.path.join(self.args.output, os.path.basename(self.args.osm))

        # Check Cache
        self.cache_dir = os.path.join(os.path.dirname(self.output), ".converter_cache")
        date_file_path = os.path.join(self.cache_dir, "file_dates.json")
        self.file_dates = dict()
        if os.path.isfile(date_file_path):
            with open(os.path.join(self.cache_dir, "file_dates.json"), 'r') as dates_file:
                self.file_dates = json.load(dates_file)
            osm_date = self.file_dates.get(os.path.basename(self.args.osm), None)
            gtfs_date = self.file_dates.get(os.path.basename(self.args.gtfs), None)
            if osm_date and gtfs_date:
                latest_osm_date = os.stat(self.args.osm).st_mtime
                latest_gtfs_date = os.stat(self.args.gtfs).st_mtime
                if latest_gtfs_date > gtfs_date or latest_osm_date > osm_date:
                    self.cached = False
                else:
                    self.cached = True
            else:
                self.cached = False
        else:
            self.cached = False
            if not os.path.isdir(self.cache_dir):
                os.makedirs(self.cache_dir)
        if self.cached:
            self._load_extended_osm()
        else:
            self.osm_parser = OSMParser(self.args.osm)

        self.gtfs_parser = GTFSParser(self.args.gtfs, self.args.begin, self.args.end)

    def run(self):
        if self.cached:
            print("Use cached data.")
            self._load_node_cache()
            schedule_converter = ScheduleConverter(self.output, self.gtfs_parser, self.osm_parser)
            # schedule_converter.extract_stations()
            # schedule_converter.extract_switches()
            schedule_converter.extract_routes()
        else:
            station_ids = OSMExtender(self.osm_parser).extend_with_gtfs_station(self.gtfs_parser, self.args.filter, self.args.distance)
            self._store_extended_osm()
            wkt_converter = GPS2WKT(self.osm_parser, self.gtfs_parser, os.path.splitext(self.output)[0] + '-extended' + '.wkt', self.args.no_scale, self.args.max_x, self.args.max_y)
            wkt_converter.transform()
            wkt_converter.osm2wkt(station_ids)
            schedule_converter = ScheduleConverter(self.output, self.gtfs_parser, self.osm_parser)
            # schedule_converter.extract_stations()
            # schedule_converter.extract_switches()
            self._write_date_cache()
            self._write_node_cache()

    def _store_extended_osm(self):
        """
        Store the OSM data as OSM XML for cache purposes.
        :return:
        """

        self.osm_parser.store(self._get_extended_osm_path())

    def _get_extended_osm_path(self):
        osm_file = os.path.basename(self.output)
        osm_name, osm_ext = os.path.splitext(osm_file)
        filename = osm_name + '-extended' + osm_ext
        return os.path.join(self.cache_dir, filename)

    def _load_extended_osm(self):
        self.osm_parser = OSMParser(self._get_extended_osm_path())

    def _write_date_cache(self):
        print("Write Cache...")
        osm_file = os.path.basename(self.args.osm)
        gtfs_file = os.path.basename(self.args.gtfs)
        latest_osm_date = os.stat(self.args.osm).st_mtime
        latest_gtfs_date = os.stat(self.args.gtfs).st_mtime
        self.file_dates[osm_file] = latest_osm_date
        self.file_dates[gtfs_file] = latest_gtfs_date
        date_file_path = os.path.join(self.cache_dir, "file_dates.json")
        with open(date_file_path, 'w') as f:
            json.dump(self.file_dates, f)

    def _load_node_cache(self):
        project = os.path.splitext(os.path.basename(self.args.osm))[0]
        NodeList.load_from_cache(self.cache_dir, project)

    def _write_node_cache(self):
        project = os.path.splitext(os.path.basename(self.args.osm))[0]
        NodeList.store_to_cache(self.cache_dir, project)


if __name__ == "__main__":
    converter = PublicTransportConverter()
    converter.run()

