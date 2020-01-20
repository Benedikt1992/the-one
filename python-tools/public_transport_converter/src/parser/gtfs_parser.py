import os
import pygtfs
from decimal import *


class GTFSParser:
    #TODO extend with filter stuff from BP and reading from gtfs raw files
    def __init__(self, gtfs_path):
        os.path.isfile(gtfs_path)
        self.path = gtfs_path
        if not os.path.isfile(gtfs_path) or os.path.splitext(gtfs_path)[1] != '.sqlite':
            raise ValueError("{} is not a file".format(gtfs_path))
        self.schedule = pygtfs.Schedule(gtfs_path)
        self.wkt_stops = {}

    def get_stops(self, coord='gps'):
        """
        This function returns stop elements from the gtfs data. Optionally with wkt coordinates.
        :return: dict {id: (lat, lon)}
        """
        stops = {}
        if coord == 'gps':
            precision = Decimal('1.000000')
            for stop in self.schedule.stops:
                stops[stop.stop_id] = (Decimal(stop.stop_lat).quantize(precision), Decimal(stop.stop_lon).quantize(precision))
        else:
            if not self.wkt_stops:
                raise ValueError("No wkt coordinates available.")
            stops = self.wkt_stops
        return stops

    def reload(self):
        self.schedule = pygtfs.Schedule(self.path)

    def update_stop_positions(self, stops):
        self.wkt_stops = stops
