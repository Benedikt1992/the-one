import os

import math
import matplotlib.pyplot as plt

from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.reader.node_location_reader import NodeLocationReader


class DistanceDeliverytimeGraph:
    """
    Show a correlation of the distance between the source and destination of a message
    and the time the message was delivered.
    """
    def __init__(self, delivered_messages: DeliveredMessagesReportReader, node_locations: NodeLocationReader, no_title,format):
        self.delivered_messages = delivered_messages
        self.locations = node_locations
        self.no_title = no_title
        self.format = format

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
        ax.scatter(x, y, c='black')
        if not self.no_title:
            ax.set_title("Delivery time / Distance Correlation")
        ax.set_ylabel("time in s", fontsize=14)
        ax.set_xlabel("distance in m", fontsize=14)
        self.__store_figure(output_path, scenario)

    @staticmethod
    def _linear_distance(coord1, coord2):
        return math.sqrt((coord1[0]-coord2[0])**2 + (coord1[1]-coord2[1])**2)

    def __store_figure(self, output, scenario):
        outputpath = os.path.join(output, scenario + "_delivery_distance_graph." + self.format)
        plt.tight_layout()
        plt.savefig(outputpath, format=self.format)
        plt.clf()  # clear plot window
        plt.close('all')