from geopy import distance
import networkx as nx

from src.parser.osm_parser import OSMParser


class GPS2WKT:
    def __init__(self, osm_parser: OSMParser, gtfs_parser, file):
        self.output = file
        self.osm_parser = osm_parser
        self.gtfs_parser = gtfs_parser
        self.minlat, self.minlon, self.maxlat, self.maxlon = osm_parser.get_bounds()

    def osm2wkt(self):
        nodes = self.osm_parser.get_nodes()

        # calculate wkt coordinates
        for node in nodes:
            lat = nodes[node][0]
            lon = nodes[node][1]
            x = distance.distance((lat, lon),
                                  (lat, self.minlon)).meters
            y = distance.distance((lat, lon),
                                  (self.minlat, lon)).meters
            nodes[node] = (lat, lon, x, y)

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
        # TODO test if gtfs stations are still part of the graph

        self._write_wkt(nodes, ways)

    def gtfs2wkt(self):
        # TODO transform coordinates within gtfs schedule
        raise NotImplementedError

    def _write_wkt(self, nodes, ways):
        with open(self.output, 'w') as output:
            for way in ways:
                output.write("LINESTRING (")
                waypoints = []
                for point in way:
                    node = nodes[point]
                    waypoints.append("{} {}".format(node[2], node[3]))

                output.write(", ".join(waypoints))
                output.write(")\n")

