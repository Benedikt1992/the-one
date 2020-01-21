import os
import pygtfs
from decimal import *

from src.elements.node import Node
from src.elements.node_list import NodeList


class GTFSParser:
    #TODO extend with filter stuff from BP and reading from gtfs raw files
    def __init__(self, gtfs_path):
        os.path.isfile(gtfs_path)
        self.path = gtfs_path
        if not os.path.isfile(gtfs_path) or os.path.splitext(gtfs_path)[1] != '.sqlite':
            raise ValueError("{} is not a file".format(gtfs_path))
        self.schedule = pygtfs.Schedule(gtfs_path)
        self.wkt_stops = {}

    def get_stops(self):
        """
        This function returns stop elements from the gtfs data. Optionally with wkt coordinates.
        :return: dict {id: Node()}
        # todo just return a list of Nodes
        """
        stops = {}

        precision = Decimal('1.000000')
        for stop in self.schedule.stops:
            node = Node.from_gtfs(stop.stop_id, Decimal(stop.stop_lat).quantize(precision), Decimal(stop.stop_lon).quantize(precision))
            node = NodeList().add_node(node)
            stops[stop.stop_id] = node

        return stops

    def reload(self):
        self.schedule = pygtfs.Schedule(self.path)
