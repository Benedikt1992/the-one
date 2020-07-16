import os
import re
from decimal import *
import networkx as nx
import math
import matplotlib.pyplot as plt
from tqdm import tqdm

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.reader.one_settings_reader import ONESettingsReader
from src.statistics import Statistics


class DeliveryCumulationGraph:
    """
    Show the cumulation of packages over time at the destination of the packages.
    Each destinantion is plotted in its own graph.
    """
    def __init__(self, delivery_reader: DeliveredMessagesReportReader, creation_reader: CreatedMessagesReportReader, settings: ONESettingsReader, no_title, format):
        self.delivered_messages_reader = delivery_reader
        self.created_messages_reader = creation_reader
        self.settings = settings
        self.threshold_toplist = {}
        self.area_toplist = {}
        self.no_title = no_title
        self.format = format

    def list_missing_messages(self, output_path, scenario):
        message_origin_map, messages, missing_msg = self.__calculate_missing_messages()
        with open(os.path.join(output_path, scenario + '_missing-messages.txt'), 'w') as file:
            file.write("# Missing {} messages out of {}:\n".format(len(missing_msg), len(messages)))
            Statistics().add_delivery_summary(len(messages), len(messages) - len(missing_msg))
            for message in missing_msg:
                file.write("{} {}\n".format(message, message_origin_map[message]))

    def __calculate_missing_messages(self):
        messages = self.created_messages_reader.get_messages()
        message_origin_map = self.created_messages_reader.get_message_origins()
        delivered_messages = self.delivered_messages_reader.get_delivered_messages()
        missing_msg = messages - delivered_messages
        return message_origin_map, messages, missing_msg

    def analyze_missing_messages(self):
        origins, _, missing_messages = self.__calculate_missing_messages()
        missing_senders = {}
        pattern = r'^(?P<group>[a-zA-Z]+)(?P<address>[0-9]+)$'
        for message in missing_messages:
            matches = re.match(pattern, origins[message])
            group = matches.group('group')
            address = matches.group('address')
            addresses = missing_senders.get(group, [])
            addresses.append(int(address))
            missing_senders[group] = addresses
        wkt_maps = self.settings.get_simulation_maps()
        schedule_files = self.settings.get_schedules()
        stationary_nodes = self.settings.get_stationary_nodes()

        locations = set()
        for group, addresses in missing_senders.items():
            locations = locations.union(self.__get_locations(addresses, stationary_nodes[group]['start'], stationary_nodes[group]['location']))
        graph = self.__build_graph(wkt_maps)
        routes = self.__build_routes(schedule_files)

        print("start searching for switch occurences...\n")
        results = {}
        for route in tqdm(routes):
            for i in range(1, len(route)):
                way = set(nx.dijkstra_path(graph, route[i - 1], route[i], lambda u, v, _: u.distance(v)))
                contained_locations = way.intersection(locations)
                if len(contained_locations) > 0:
                    for location in contained_locations:
                        occurences = results.get(location, set())
                        occurences.add((route[i - 1], route[i]))
                        results[location] = occurences
                    # print("Path from {} to {} contains switches {}\n".format(route[i - 1], route[i],
                    #                                                          {str(x) for x in contained_locations}))
        if len(results) > 0:
            print("Some of the missing messages can be received. The following list shows which routes contain the sending node:\n\n")
            for sender in results.keys():
                print("Messages sent from {} are contained in the path between the following stop positions:\n".format(sender))
                for fromNode, toNode in results[sender]:
                    print("\tFrom {} to {}\n".format(fromNode, toNode))
        else:
            print("It is not possible to receive any of the missing messages.\n")

    def create_all_from_scenario(self, output_path, scenario):
        destinations = self.created_messages_reader.get_messages_grouped_by_destination()
        for destination, messages in destinations.items():
            self.create_station_from_scenario(output_path, scenario, destination, messages)
        self.__store_toplist(output_path, scenario)

    def create_station_from_scenario(self, output_path, scenario, destination, messages):
        array = self.__get_cumulation_array(destination)
        maximum = len(messages)

        area = 0
        for i in range(0, len(array)):
            # TODO make threashold configurable
            area += maximum - array[i]
            if array[i] > 0.85 * maximum:
                self.__add_to_toplist(destination, i, 'threshold')
        self.__add_to_toplist(destination, area, 'area')
        Statistics().add_area_metric(destination, area)

        x_axis = [0] * len(array)
        for i in range(len(array)):
            x_axis[i] = i / (60.0)
        plt.plot(x_axis, array)

        if not self.no_title:
            plt.title('collected station data at {}'.format(destination))
        plt.ylabel('Messages', fontsize=14)
        plt.xlabel('hours', fontsize=14)
        axes = plt.gca()
        axes.set_ylim([0, maximum * 1.01])
        axes.set_yticks(list(plt.yticks()[0]) + [maximum])

        axes.text(0, maximum, '< max', verticalalignment='center', horizontalalignment='left')
        self.__store_figure(output_path, scenario, destination)

    def __get_cumulation_array(self, destination):
        deliveries = self.delivered_messages_reader.get_deliveries(destination)
        deliveries = sorted(deliveries, key=lambda x: x[0])
        max_time = self.settings.get_simulation_duration()
        cumulative_array = [0] * max_time
        cumulation = 0
        for delivery in deliveries:
            cumulation += 1
            cumulative_array[int(delivery[0])] = cumulation

        cumulation = cumulative_array[0]
        for i in range(1, max_time):
            if cumulative_array[i] > cumulative_array[i-1]:
                cumulation = cumulative_array[i]
            cumulative_array[i] = cumulation

        return cumulative_array

    def __store_figure(self, output, scenario, destination):
        outputpath = os.path.join(output, scenario + "_delivery-cumulation_" + destination + '.' + self.format)
        plt.tight_layout()
        plt.savefig(outputpath, format=self.format)
        plt.clf()  # clear plot window
        plt.close('all')

    def __add_to_toplist(self, station, value, type):
        if type == 'threshold':
            self.threshold_toplist[station] = value
        elif type == 'area':
            self.area_toplist[station] = value

    def __store_toplist(self, output, scenario):
        outputpath = os.path.join(output, scenario + "_delivery-toplist-threshold.txt")
        with open(outputpath, "w") as file:
            for key, value in self.threshold_toplist.items():
                file.write("{},{}\n".format(key, str(value)))

        outputpath = os.path.join(output, scenario + "_delivery-toplist-area.txt")
        with open(outputpath, "w") as file:
            for key, value in self.area_toplist.items():
                file.write("{},{}\n".format(key, str(value)))

    def __build_graph(self, wkt_maps):
        graph = nx.Graph()
        for wkt_map in wkt_maps:
            with open(wkt_map, 'r') as file:
                for line in file:
                    waypoints = line.strip()[12:-1].split(', ')
                    waynodes = []
                    for point in waypoints:
                        x, y = point.split(' ')
                        waynodes.append(self.Node(x, y))

                    for i in range(1, len(waynodes)):
                        graph.add_edge(waynodes[i - 1], waynodes[i])
        return graph

    def __build_routes(self, schedule_files):
        routes = []
        for schedule in schedule_files:
            with open(schedule, 'r') as file:
                for line in file:
                    route = []
                    for stop in line.strip()[7:-1].split(', '):
                        t, x, y = stop.split(' ')
                        route.append(self.Node(x, y))
                    routes.append(route)
        return routes

    def __get_locations(self, addresses, first_address, address_locations):
        addresses = sorted(addresses)
        lines = [address - first_address for address in addresses]
        locations = set()
        with open(address_locations, 'r') as file:
            for i, line in enumerate(file):
                if i in lines:
                    x, y = line.strip()[7:-1].split(' ')
                    locations.add(self.Node(x, y))
        return locations

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
