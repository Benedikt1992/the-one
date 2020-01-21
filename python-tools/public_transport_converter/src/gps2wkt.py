from geopy import distance
import networkx as nx
from decimal import *

from src.elements.node_list import NodeList
from src.parser.osm_parser import OSMParser


class GPS2WKT:
    def __init__(self, osm_parser: OSMParser, gtfs_parser, file,  no_scale, max_x, max_y):
        self.output = file
        self.osm_parser = osm_parser
        self.gtfs_parser = gtfs_parser
        self.minlat, self.minlon, self.maxlat, self.maxlon = osm_parser.get_bounds()
        self.no_scaling = no_scale
        self.max_scaled_x = max_x
        self.max_scaled_y = max_y

    def transform(self):
        scaling_factor = self.scaling_factor()

        for node in NodeList():
            lat = node.lat
            lon = node.lon
            x = distance.distance((lat, lon),
                                  (lat, self.minlon)).meters / scaling_factor
            y = distance.distance((lat, lon),
                                  (self.minlat, lon)).meters / scaling_factor
            node.update_wkt(x, y)

    def osm2wkt(self, station_ids):

        ways = self.osm_parser.get_ways()

        # check if graph is completely connected
        graph = nx.Graph()
        for way in ways:
            for i in range(len(way)):
                if i == 0:
                    continue
                graph.add_edge(way[i - 1], way[i])

        connected_sets = list(nx.connected_components(graph))
        largest_set = max(connected_sets, key=len)
        connected_sets.remove(largest_set)
        for partition in connected_sets:
            r = largest_set.intersection(partition)
            if r:
                raise ArithmeticError("Partitions are not disjoint")

        for partition in connected_sets:
            for way in ways:
                if partition.intersection(way):
                    ways.remove(way)

        print("Removed {} unconnected nodes from {} nodes in total.".format(graph.number_of_nodes() - len(largest_set),
                                                                            graph.number_of_nodes()))

        graph = nx.Graph()
        for way in ways:
            for i in range(len(way)):
                if i == 0:
                    continue
                graph.add_edge(way[i - 1], way[i])
        connected_sets = list(nx.connected_components(graph))
        if len(connected_sets) > 1:
            print("Couldn't remove all partitions.")

        if not set(station_ids).issubset(graph.nodes):
            raise RuntimeError("Some GTFS stations are not part of the graph.")

        self._write_wkt(ways)

    def scaling_factor(self):
        if self.no_scaling:
            scaling_factor = int(1)
        else:
            max_x = distance.distance((self.maxlat, self.maxlon),
                                      (self.maxlat, self.minlon)).meters
            max_y = distance.distance((self.maxlat, self.maxlon),
                                      (self.minlat, self.maxlon)).meters
            scaling_factor_x = int(max_x / self.max_scaled_x) + 1
            scaling_factor_y = int(max_y / self.max_scaled_y) + 1
            scaling_factor = max(scaling_factor_x, scaling_factor_y)
            print("Scale down with factor 1:{} to meet destination size of {}x{}".format(scaling_factor,
                                                                                         self.max_scaled_x,
                                                                                         self.max_scaled_y))
        return scaling_factor

    def _write_wkt(self, ways):
        nodelist = NodeList()
        with open(self.output, 'w') as output:
            for way in ways:
                output.write("LINESTRING (")
                waypoints = []
                for point in way:
                    node = nodelist.find_by_osm_id(point)
                    # Limit precision to 6 digits
                    waypoints.append("{:.6f} {:.6f}".format(node.x, node.y))

                output.write(", ".join(waypoints))
                output.write(")\n")

