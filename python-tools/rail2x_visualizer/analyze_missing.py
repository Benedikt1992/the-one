import os
from decimal import *
import networkx as nx
import math


class Node:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.xv = Decimal(x)
        self.yv = Decimal(y)

    def distance(self, other):
        return math.sqrt((self.yv - other.yv) ** 2 + (self.xv - other.xv) ** 2)

    def __hash__(self):
        return hash(self.x + self.y)

    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    def __ne__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return "Node({} {})".format(self.x, self.y)

def print_missing_messages(scenario):
    messages = set()
    message_creators = {}
    with open(scenario + '_CreatedMessagesReport.txt', 'r') as file:
        file.readline()
        for line in file:
            line_array = line.split(' ')
            messages.add(line_array[1])
            message_creators[line_array[1]] = line_array[3]

    delivered_messages = set()
    with open(scenario + '_DeliveredMessagesReport.txt', 'r') as file:
        file.readline()
        for line in file:
            line_array = line.split(' ')
            delivered_messages.add(line_array[1])

    missing_msg = messages - delivered_messages
    with open(scenario + '_missing-messages.txt', 'w') as file:
        file.write("# Missing {} messages out of {}:\n".format(len(missing_msg), len(messages)))
        for message in missing_msg:
            file.write("{} {}\n".format(message, message_creators[message]))


def test_switch_participation(switch_list, wkt_map, train_routes, stop_locations, switch_locations):
    graph = _build_graph(wkt_map)
    routes = _build_routes(train_routes)
    no_stops = 0
    with open(stop_locations) as f:
        for no_stops,_ in enumerate(f):
            pass
    no_stops += 1
    no_routes = len(routes)
    switches = _get_switches(switch_list, no_stops + no_routes, switch_locations)

    print("start searching for switch occurences...\n")
    for route in routes:
        for i in range(1, len(route)):
            way = set(nx.dijkstra_path(graph, route[i-1], route[i], lambda u, v, _: u.distance(v)))
            contained_switches = way.intersection(switches)
            if len(contained_switches) > 0:
                print("Path from {} to {} contains switches {}\n".format(route[i-1], route[i], {str(x) for x in contained_switches}))


def _build_graph(wkt_map):
    graph = nx.Graph()
    with open(wkt_map, 'r') as file:
        for line in file:
            waypoints = line.strip()[12:-1].split(', ')
            waynodes = []
            for point in waypoints:
                x, y = point.split(' ')
                waynodes.append(Node(x, y))

            for i in range(1, len(waynodes)):
                graph.add_edge(waynodes[i - 1], waynodes[i])
    return graph


def _build_routes(train_routes):
    routes = []
    with open(train_routes, 'r') as file:
        for line in file:
            route = []
            for stop in line.strip()[7:-1].split(', '):
                t,x,y = stop.split(' ')
                route.append(Node(x, y))
            routes.append(route)
    return routes


def _get_switches(switch_list, first_address, switch_locations):
    switch_list = sorted(switch_list)
    lines = [address - first_address for address in switch_list]
    switches = set()
    with open(switch_locations, 'r') as file:
        for i, line in enumerate(file):
            if i in lines:
                x, y = line.strip()[7:-1].split(' ')
                switches.add(Node(x, y))
    return switches



if __name__  == "__main__":
    os.chdir('C:\\reports_2018_N')
    scenario = 'rail2x-2018-R_EpidemicRouter-D_NoDelay-E_458, 459'
    # print_missing_messages(scenario)
    switch_list = [606, 884, 517, 810, 660, 639, 563, 890, 738, 482, 760]
    wkt_map = 'C:\\ICE-germany-extended.wkt'
    train_routes = 'C:\\ICE-germany-routes.txt'
    stop_locations = 'C:\\ICE-germany-stations.wkt'
    switch_locations = 'C:\\ICE-germany-switches.wkt'
    test_switch_participation(switch_list, wkt_map, train_routes, stop_locations, switch_locations)
