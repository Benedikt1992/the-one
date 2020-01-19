import os

from src.parser.gtfs_parser import GTFSParser


class ScheduleConverter:
    def __init__(self, output, gtfs_parser: GTFSParser):
        self.output_dir = os.path.dirname(output)
        self.project_name = os.path.splitext(os.path.basename(output))[0]
        self.gtfs_parser = gtfs_parser

    def extract_stations(self):
        # TODO start with an already converted map and gtfs
        stations = self.gtfs_parser.get_stops('wkt')
        with open(os.path.join(self.output_dir, self.project_name + "-stations.wkt"), 'w') as file:
            for station in stations.values():
                file.write("POINT ({} {})\n".format(str(station[0]), str(station[1])))
