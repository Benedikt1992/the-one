import os
import networkx as nx
import math

from src.elements.node_list import NodeList
from src.elements.route import Route
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
                file.write("POINT ({:.6f} {:.6f})\n".format(node.x, node.y))

    @staticmethod
    def distance(node1, node2, attributes):
        nodes = NodeList()
        node1 = nodes.find_by_osm_id(node1)
        node2 = nodes.find_by_osm_id(node2)
        dx = node1.x - node2.x
        dy = node1.y - node2.y

        return math.sqrt(dx*dx + dy*dy)

    def extract_routes(self):
        trips = self.gtfs_parser.get_trips()
        max_time = trips[-1].end()
        routes = {}
        for trip in trips:
            waiting_routes = routes.get(trip.first_stop(), [])
            if trip.end() > max_time:
                max_time = trip.end()
            found = False
            for route in waiting_routes:
                if route < trip:
                    route += trip
                    routes[trip.first_stop()].remove(route)
                    route_list = routes.get(trip.last_stop(), [])
                    route_list.append(route)
                    routes[trip.last_stop()] = route_list
                    found = True
                    break
            if not found:
                route_list = routes.get(trip.last_stop(), [])
                route_list.append(Route().append(trip))
                routes[trip.last_stop()] = route_list

        simulation_duration = (max_time - self.gtfs_parser.get_start_date()).total_seconds() / 60
        print("The simulation end time for this schedule is {}".format(round(0.5 + simulation_duration))) # round up

        with open(os.path.join(self.output_dir, self.project_name + "-routes.txt"), 'w') as file:
            for route_list in routes.values():
                for route in route_list:
                    file.write("{}\n".format(str(route)))


