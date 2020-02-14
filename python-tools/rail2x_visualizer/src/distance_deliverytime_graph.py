import os

import math
import matplotlib.pyplot as plt

from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.reader.node_location_reader import NodeLocationReader


class DistanceDeliverytimeGraph:
    def __init__(self, delivered_messages: DeliveredMessagesReportReader, node_locations: NodeLocationReader):
        self.delivered_messages = delivered_messages
        self.locations = node_locations

    def create_scatter_plot(self, output_path, scenario):
        x = []
        y = []
        for delivery in self.delivered_messages:
            fromHost = self.locations.get_node_location(delivery['from'])
            toHost = self.locations.get_node_location(delivery['to'])
            if fromHost is None or toHost is None:
                raise ValueError("At least one Host-Location couldn't be found.")
            x.append(self._linear_distance(fromHost, toHost))
            y.append(delivery['time'])

        fig, ax = plt.subplots()
        ax.scatter(x, y)
        ax.set_title("Delivery time / Distance Correlation")
        ax.set_ylabel("time in s")
        ax.set_xlabel("distance in m")
        self.__store_figure(output_path, scenario)

    @staticmethod
    def _linear_distance(coord1, coord2):
        return math.sqrt((coord1[0]-coord2[0])**2 + (coord1[1]-coord2[1])**2)

    @staticmethod
    def __store_figure(output, scenario, format='svg'):
        outputpath = os.path.join(output, scenario + "_delivery_distance_graph." + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window