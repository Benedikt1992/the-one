import os
import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.statistics import Statistics


class HopDistribution:
    """
    This class visualizes the hops the messages needed in order to reach their destination.
    """
    def __init__(self, delivery_reader: DeliveredMessagesReportReader, creation_reader: CreatedMessagesReportReader, no_title, format):
        self.delivered_messages_reader = delivery_reader
        self.created_messages_reader = creation_reader
        self.no_title = no_title
        self.format = format

    def create_histogram_from_scenario(self, output_path, scenario):
        """
        Create a histogram showing the hop distance distribution of all messages
        :param output_path:
        :param scenario:
        :return:
        """
        boxplot_distribution = self.delivered_messages_reader.get_hops()
        if len(boxplot_distribution) > 0:
            mode = max(boxplot_distribution, key=boxplot_distribution.count)
            minimum = min(boxplot_distribution)
            maximum = max(boxplot_distribution)
            avg = sum(boxplot_distribution) / len(boxplot_distribution)
            Statistics().set_hop_stats(minimum, maximum, mode, avg)
        plt.hist(boxplot_distribution, bins=50)
        if not self.no_title:
            plt.title('Hop Distribution of all Messages')
        plt.ylabel('Occurences', fontsize=14)
        plt.xlabel('Hops', fontsize=14)
        self.__store_figure(output_path, scenario, "hop-histogram", "all")

    def create_boxplots_from_scenario(self, output_path, scenario):
        """
        Create a boxplot for each destination showing the distribution of hops of messages destined to this node.
        :param output_path:
        :param scenario:
        :return:
        """
        destinations = list(self.created_messages_reader.get_destinations())
        destinations = sorted(destinations,  key=lambda x: (len(x), x))
        data = []
        tick_labels = []
        for destination in destinations:
            tick_labels.append(destination)
            data.append(self.delivered_messages_reader.get_hops(destination))

        boxplot_width = 0.7
        fig, axs = plt.subplots(figsize=(10, boxplot_width*len(destinations)))
        axs.boxplot(data, vert=False)
        if not self.no_title:
            axs.set_title("Boxplots of all destinations")
        axs.set_ylabel('Destinations', fontsize=14)
        axs.set_xlabel('Hops', fontsize=14)
        axs.set_yticklabels(tick_labels)

        self.__store_figure(output_path, scenario, "hop-boxplots", "all")

    def __store_figure(self, output, scenario, type, destination):
        outputpath = os.path.join(output, scenario + "_" + type + "_" + destination + '.' + self.format)
        plt.tight_layout()
        plt.savefig(outputpath, format=self.format)
        plt.clf()  # clear plot window
        plt.close('all')

