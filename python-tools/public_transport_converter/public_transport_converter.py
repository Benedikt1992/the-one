import os
from argparse import ArgumentParser

from src.parser.osm_parser import OSMParser
from src.parser.gtfs_parser import GTFSParser
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
        parser.add_argument('-o', '--output', nargs='?', default='', help="Output file. The source OSM File is extended with '-extended' by default.")
        parser.add_argument('-s', '--no-scale', action='store_true', default=False, help="Disable scaling of the map for simulation.")
        parser.add_argument('-x', '--max_x', nargs="?", default='4500', type=int, help="Maximum size of x dimension within simulation.")
        parser.add_argument('-y', '--max_y', nargs="?", default='3400', type=int, help="Maximum size of y dimension within simulation.")
        # TODO redefine output parameter for all outputs
        self.args = parser.parse_args()

        self.osm_parser = OSMParser(self.args.osm)
        self.gtfs_parser = GTFSParser(self.args.gtfs)
        if not self.args.output:
            self.output = os.path.splitext(self.args.osm)[0] + '-extended' + os.path.splitext(self.args.osm)[1]
        else:
            self.output = self.args.output

    def run(self):
        OSMExtender(self.osm_parser).extend_with_gtfs_station(self.gtfs_parser, self.args.filter, self.args.distance)
        self._store_osm()
        GPS2WKT(self.osm_parser, self.gtfs_parser, os.path.splitext(self.output)[0] + '.wkt', self.args.no_scale, self.args.max_x, self.args.max_y).osm2wkt()

    def _store_osm(self):
        """
        Store the OSM data as OSM XML.
        :return:
        """
        self.osm_parser.store(self.output)


if __name__ == "__main__":
    converter = PublicTransportConverter()
    converter.run()

