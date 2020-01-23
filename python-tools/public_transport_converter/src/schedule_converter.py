import os
import networkx as nx
import math

from src.elements.node_list import NodeList
from src.parser.gtfs_parser import GTFSParser
from src.parser.osm_parser import OSMParser


class ScheduleConverter:
    def __init__(self, output, gtfs_parser: GTFSParser, osm_parser: OSMParser):
        self.output_dir = os.path.dirname(output)
        self.project_name = os.path.splitext(os.path.basename(output))[0]
        self.gtfs_parser = gtfs_parser
        self.osm_parser = osm_parser

    def extract_stations(self):
        stations = self.gtfs_parser.get_stops().values()
        self.write_stationary_nodes(stations, "stations")

    def extract_switches(self):
        ways = self.osm_parser.get_ways()

        # check if graph is completely connected
        graph = nx.Graph()
        for way in ways:
            for i in range(len(way)):
                if i == 0:
                    continue
                graph.add_edge(way[i - 1], way[i])

        sections = self.gtfs_parser.get_sections()
        nodes = NodeList()
        switches = set()

        for section in sections:
            node1 = nodes.find_by_gtfs_id(section[0])
            node2 = nodes.find_by_gtfs_id(section[1])
            path = nx.dijkstra_path(graph, node1.osm_id, node2.osm_id, self.distance)
            switch_id = path[int(len(path) / 2)]
            switch = nodes.find_by_osm_id(switch_id)
            switches.add(switch)

        self.write_stationary_nodes(switches, "switches")

    def write_stationary_nodes(self, nodes, node_type):
        with open(os.path.join(self.output_dir, self.project_name + "-" + node_type + ".wkt"), 'w') as file:
            for node in nodes:
                if (not node.x) or (not node.y):
                    raise ValueError("No wkt coordinates available: {}".format(str(node)))
                file.write("POINT ({} {})\n".format(str(node.x), str(node.y)))

    @staticmethod
    def distance(node1, node2, attributes):
        nodes = NodeList()
        node1 = nodes.find_by_osm_id(node1)
        node2 = nodes.find_by_osm_id(node2)
        dx = node1.x - node2.x
        dy = node1.y - node2.y

        return math.sqrt(dx*dx + dy*dy)
