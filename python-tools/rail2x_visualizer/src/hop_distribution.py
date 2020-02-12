import os
import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader


class HopDistribution:
    def __init__(self, delivery_reader: DeliveredMessagesReportReader, creation_reader: CreatedMessagesReportReader):
        self.delivered_messages_reader = delivery_reader
        self.created_messages_reader = creation_reader

    def create_histogram_from_scenario(self, output_path, scenario):
        boxplot_distribution = self.delivered_messages_reader.get_hops()
        plt.hist(boxplot_distribution, bins=50)
        plt.title('Hop Distribution of all Messages')
        plt.ylabel('Occurences')
        plt.xlabel('Hops')
        self.__store_figure(output_path, scenario, "hop-histogram", "all")

    def create_boxplots_from_scenario(self, output_path, scenario):
        destinations = self.created_messages_reader.get_destinations()
        data = []
        tick_labels = []
        ticks = []
        tick = 1
        for destination in destinations:
            ticks.append(tick)
            tick += 1
            tick_labels.append(destination)
            data.append(self.delivered_messages_reader.get_hops(destination))

        #TODO fix size of out put. Currently it starts to overlap
        plt.boxplot(data, vert=False)
        plt.title("Boxplots of all destinations")
        plt.ylabel('Destinations')
        plt.xlabel('Hops')
        plt.yticks(ticks, tick_labels)

        self.__store_figure(output_path, scenario, "hop-boxplots", "all")

    @staticmethod
    def __store_figure(output, scenario, type, destination, format='svg'):
        outputpath = os.path.join(output, scenario + "_" + type + "_" + destination + '.' + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window

