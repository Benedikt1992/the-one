import os

import matplotlib.pyplot as plt

from src.reader.message_processing_report_reader import MessageProcessingReportReader
from src.reader.message_snapshot_report_reader import MessageSnapshotReportReader


class NodeLoad:
    """
    Show the load on nodes.
    """
    def __init__(self, processing_report: MessageProcessingReportReader, snapshot_report: MessageSnapshotReportReader):
        self.processing_report = processing_report
        self.snapshot_report = snapshot_report

    def load_distribution_by_hostgroup(self, output_path, scenario):
        """
        Show the distribution of sent and received messages sorted by host group
        :param output_path:
        :param scenario:
        :return:
        """
        groups = self.processing_report.get_host_groups()
        data = []
        tick_labels = []
        for group in groups:
            tick_labels.append(group + '_incoming')
            tick_labels.append(group + '_outgoing')
            data.append(self.processing_report.get_incoming_distribution(group))
            data.append(self.processing_report.get_outgoing_distribution(group))

        boxplot_width = 1
        fig, axs = plt.subplots(figsize=(10, boxplot_width * len(data)))
        axs.boxplot(data, vert=False)
        axs.set_title("Distribution of transferred messages")
        axs.set_xlabel('transferred messages')
        axs.set_yticklabels(tick_labels)

        self.__store_figure(output_path, scenario, 'processed-messages')

    def load_timeline(self, output_path, scenario):
        """
        Show hte amount of carried messages during the simulation.
        Each hostgroup is plotted within one graph.
        :param output_path:
        :param scenario:
        :return:
        """
        groups = self.snapshot_report.get_host_groups()

        for group in groups:
            self.load_timeline_hostgroup(output_path, scenario, group)

    def load_timeline_hostgroup(self, output_path, scenario, group):
        intervals, lines = self.snapshot_report.get_lines(group)

        for line in lines:
            plt.plot(intervals, line, color='black', linewidth=0.5,)

        plt.title("Load within host group " + group)
        plt.xlabel("time in minutes")
        plt.ylabel("carried messages")
        self.__store_figure(output_path, scenario, "load_timeline_" + group)

    @staticmethod
    def __store_figure(output, scenario, type, format='svg'):
        outputpath = os.path.join(output, scenario + "_" + type + "." + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window

