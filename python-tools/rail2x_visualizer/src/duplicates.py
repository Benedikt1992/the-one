import os

import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.message_duplicates_report_reader import MessageDuplicatesReportReader


class Duplicates:
    def __init__(self, duplicates_reader: MessageDuplicatesReportReader, creation_reader: CreatedMessagesReportReader):
        self.duplicates_reader = duplicates_reader
        self.created_messages_reader = creation_reader

    def duplicates_heatmap(self, output_path, scenario, node_prefix=""):
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
        ax.set_title('Duplicates per Message on each {}'.format(node_prefix))
        ax.set_xlabel(node_prefix + 's')
        ax.set_ylabel('Messages')
        cbar.ax.set_ylabel("Duplicates")
        if len(messages) > 1000:
            print("Too many messages. Store heatmap in pixel format.")
            self.__store_figure(output_path, scenario, node_prefix, 'png')
        else:
            self.__store_figure(output_path, scenario, node_prefix)

    @staticmethod
    def __store_figure(output, scenario, prefix, format='svg'):
        outputpath = os.path.join(output, scenario + "_duplicates_heatmap_" + prefix + '.' + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window







