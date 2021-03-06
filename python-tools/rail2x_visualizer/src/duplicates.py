import os

import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.message_duplicates_report_reader import MessageDuplicatesReportReader


class Duplicates:
    """
    Show how often a message was processed by a node in a heatmap.
    """
    def __init__(self, duplicates_reader: MessageDuplicatesReportReader, creation_reader: CreatedMessagesReportReader, no_title, format):
        self.duplicates_reader = duplicates_reader
        self.created_messages_reader = creation_reader
        self.no_title = no_title
        self.format = format

    def duplicates_heatmap(self, output_path, scenario, node_prefix=""):
        """
        See class description
        :param output_path:
        :param scenario:
        :param node_prefix: optionally a prefix to filter the nodes displayed within the heatmap.
        :return:
        """
        messages = sorted(list(self.created_messages_reader.get_messages()))
        duplicates = self.duplicates_reader.get_duplicates_grouped_by_host(node_prefix)
        if not duplicates:
            return # no nodes with prefix found

        for key in duplicates.keys():
            duplicates[key] = sorted(duplicates[key], key=lambda x: x[0])

        hosts = sorted(duplicates.keys(), key=lambda x: (len(x), x))
        data = []
        for host in hosts:
            for i in range(len(messages)):
                try:
                    if messages[i] != duplicates[host][i][0]:
                        duplicates[host].insert(i, (messages[i], 0))
                except IndexError:
                    duplicates[host].append((messages[i], 0))
            data.append([n[1] for n in duplicates[host]])
        # transpose data
        data = list(map(list, zip(*data)))

        fig, ax = plt.subplots()
        c = ax.pcolormesh(data, cmap='Reds')
        cbar = fig.colorbar(c, ax=ax)  # need a colorbar to show the intensity scale
        if not self.no_title:
            ax.set_title('Duplicates per Message on each {}'.format(node_prefix))
        ax.set_xlabel(node_prefix + 's', fontsize=14)
        ax.set_ylabel('Messages', fontsize=14)
        cbar.ax.set_ylabel("Duplicates", fontsize=14)
        if len(messages) > 1000:
            print("Too many messages. Store heatmap in pixel format.")
            self.__store_figure(output_path, scenario, node_prefix, 'png')
        else:
            self.__store_figure(output_path, scenario, node_prefix)

    def __store_figure(self, output, scenario, prefix, format=None):
        file_format = self.format
        if format:
            file_format = format
        outputpath = os.path.join(output, scenario + "_duplicates_heatmap_" + prefix + '.' + file_format)
        plt.tight_layout()
        plt.savefig(outputpath, format=file_format)
        plt.clf()  # clear plot window
        plt.close('all')







