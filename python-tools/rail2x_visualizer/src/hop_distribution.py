import os
import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader


class HopDistribution:
    """
    This class visualizes the hops the messages needed in order to reach their destination.
    """
    def __init__(self, delivery_reader: DeliveredMessagesReportReader, creation_reader: CreatedMessagesReportReader):
        self.delivered_messages_reader = delivery_reader
        self.created_messages_reader = creation_reader

    def create_histogram_from_scenario(self, output_path, scenario):
        """
        Create a histogram showing the hop distance distribution of all messages
        :param output_path:
        :param scenario:
        :return:
        """
        boxplot_distribution = self.delivered_messages_reader.get_hops()
        plt.hist(boxplot_distribution, bins=50)
        plt.title('Hop Distribution of all Messages')
        plt.ylabel('Occurences')
        plt.xlabel('Hops')
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
        axs.set_title("Boxplots of all destinations")
        axs.set_ylabel('Destinations')
        axs.set_xlabel('Hops')
        axs.set_yticklabels(tick_labels)

        self.__store_figure(output_path, scenario, "hop-boxplots", "all")

    @staticmethod
    def __store_figure(output, scenario, type, destination, format='svg'):
        outputpath = os.path.join(output, scenario + "_" + type + "_" + destination + '.' + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window

