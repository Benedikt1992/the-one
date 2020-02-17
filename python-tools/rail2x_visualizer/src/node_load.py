import os

import matplotlib.pyplot as plt

from src.reader.message_processing_report_reader import MessageProcessingReportReader


class NodeLoad:
    def __init__(self, processing_report: MessageProcessingReportReader):
        self.processing_report = processing_report

    def load_distribution_by_hostgroup(self, output_path, scenario):
        groups = self.processing_report.get_host_groups()
        # TODO make boxplots of incoming and outgoing distribution per node group
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

        self.__store_figure(output_path, scenario)

    @staticmethod
    def __store_figure(output, scenario, format='svg'):
        outputpath = os.path.join(output, scenario + "_processed-messages." + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window

