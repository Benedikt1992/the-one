import os
import pygtfs
from decimal import *


class GTFSParser:
    #TODO extend with filter stuff from BP and reading from gtfs raw files
    def __init__(self, gtfs_path):
        os.path.isfile(gtfs_path)
        if not os.path.isfile(gtfs_path) or os.path.splitext(gtfs_path)[1] != '.sqlite':
            raise ValueError("{} is not a file".format(gtfs_path))
        self.schedule = pygtfs.Schedule(gtfs_path)

    def get_stops(self):
        stops = {}
        precision = Decimal('1.000000')
        for stop in self.schedule.stops:
            stops[stop.stop_id] = (Decimal(stop.stop_lat).quantize(precision), Decimal(stop.stop_lon).quantize(precision))
        return stops
