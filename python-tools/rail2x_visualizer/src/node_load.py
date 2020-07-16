import os

import matplotlib.pyplot as plt

from src.reader.message_processing_report_reader import MessageProcessingReportReader
from src.reader.message_snapshot_report_reader import MessageSnapshotReportReader
from src.statistics import Statistics


class NodeLoad:
    """
    Show the load on nodes.
    """
    def __init__(self, processing_report: MessageProcessingReportReader, snapshot_report: MessageSnapshotReportReader, no_title, format):
        self.processing_report = processing_report
        self.snapshot_report = snapshot_report
        self.no_title = no_title
        self.format = format

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
        incoming = 0
        incoming_len = 0
        outgoing = 0
        outgoing_len = 0
        for group in groups:
            tick_labels.append(group + '_incoming')
            tick_labels.append(group + '_outgoing')
            in_dist = self.processing_report.get_incoming_distribution(group)
            out_dist = self.processing_report.get_outgoing_distribution(group)
            data.append(in_dist)
            data.append(out_dist)
            incoming += sum(in_dist)
            incoming_len += len(in_dist)
            outgoing += sum(out_dist)
            outgoing_len += len(out_dist)
        Statistics().set_processing_stats(incoming / incoming_len, outgoing / outgoing_len)

        boxplot_width = 0.8
        fig, axs = plt.subplots(figsize=(10, boxplot_width * len(data)))
        axs.boxplot(data, vert=False)
        # axs.set_aspect(1.5)
        if not self.no_title:
            axs.set_title("Distribution of transferred messages")
        axs.set_xlabel('transferred messages', fontsize=14)
        axs.set_yticklabels(tick_labels, fontsize=14)

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

        if not self.no_title:
            plt.title("Load within host group " + group)
        plt.xlabel("time in minutes", fontsize=14)
        plt.ylabel("carried messages", fontsize=14)
        self.__store_figure(output_path, scenario, "load_timeline_" + group)

    def __store_figure(self, output, scenario, type):
        outputpath = os.path.join(output, scenario + "_" + type + "." + self.format)
        plt.tight_layout()
        plt.savefig(outputpath, format=self.format)
        plt.clf()  # clear plot window
        plt.close('all')

