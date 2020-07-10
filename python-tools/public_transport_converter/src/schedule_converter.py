import os
import re
import sys
from time import sleep

import networkx as nx
import math
from geopy.geocoders import Nominatim

from src.elements.node_list import NodeList
from src.elements.route import Route
from src.elements.section import Section
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

    def extract_switches(self, country_filter=None):
        ways = self.osm_parser.get_ways()

        graph = nx.Graph()
        for way in ways:
            for i in range(len(way)):
                if i == 0:
                    continue
                graph.add_edge(way[i - 1], way[i])

        osm_switches = self.osm_parser.get_nodes([('railway', 'switch')]).keys()
        switches = set()
        for node in osm_switches:
            if len(graph[node]) > 2:  # switch nodes with <=2 neighbors are not used as switches
                switches.add(NodeList().find_by_osm_id(node))

        if country_filter:
            geolocator = Nominatim(user_agent="one_ptc")
            false_switches = set()
            r = re.compile(country_filter)
            for node in switches:
                # todo use a progressbar like tqdm
                country = node.get_country()
                if country is not None:
                    if not r.match(country):
                        false_switches.add(node)
                else:
                    query = node.get_gps()
                    location = geolocator.reverse(query, language='en')
                    try:
                        country = location.raw['address']['country']
                    except KeyError:
                        print("This location has no associated country: " + str(location))
                        country = ""
                    if not r.match(country):
                        false_switches.add(node)
                    node.set_country(country)
                    sleep(1)  # rate limiting
            switches.difference_update(false_switches)
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
        print("Building trains based on {} trips.\n".format(len(trips)))
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
