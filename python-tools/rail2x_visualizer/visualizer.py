import os
from argparse import ArgumentParser, ArgumentError

from src.delivery_cumulation_graph import DeliveryCumulationGraph
from src.distance_deliverytime_graph import DistanceDeliverytimeGraph
from src.duplicates import Duplicates
from src.hop_distribution import HopDistribution
from src.node_load import NodeLoad
from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.reader.message_duplicates_report_reader import MessageDuplicatesReportReader
from src.reader.message_processing_report_reader import MessageProcessingReportReader
from src.reader.node_location_reader import NodeLocationReader
from src.reader.one_settings_reader import ONESettingsReader


class Visualizer:
    def __init__(self):
        parser = ArgumentParser(description='Extend osm xml data by gtfs stations and connect them to their stop points')
        parser.add_argument('-r', '--reports', required=True, help="Directory of the generated reports.")
        parser.add_argument('-o', '--output', nargs='?', default='', help = "Output directory. The directory of the reports is used by default.")
        parser.add_argument('-d', '--heatmap', nargs="*",
                            default=[""],
                            help="Define for which nodes a heatmap should be created showing the duplicate processing on these nodes."
                                 " Default is all nodes in on graph. Can have a list of different node prefixes.")
        parser.add_argument('-s', '--simulation', required=True,
                            help="Path of the simulation working directory. "
                                 "This is used read files referenced withing the simulation configuration.")
        self.args = parser.parse_args()

        if not os.path.isdir(self.args.reports):
            raise ArgumentError("-r/--reports needs to be a directory.")

        if not os.path.isdir(self.args.simulation):
            raise ArgumentError("-s/--simulation needs to be a directory.")

        if not self.args.output:
            self.output = self.args.reports
        else:
            if not os.path.isdir(self.args.output):
                os.makedirs(self.args.output)
            self.output = self.args.output

        self.delivered_messages_report = None
        self.created_messages_report = None
        self.message_duplicates_report = None
        self.message_processing_report = None

        # Settings
        self.settings = os.path.join(self.args.reports, "settings.txt")
        if not os.path.isfile(self.settings):
            raise ValueError(
                "Settings are not available at {}".format(self.settings))

    def run(self):
        settings_reader = ONESettingsReader(self.settings, self.args.simulation)
        while settings_reader.next_run() is not None:
            scenario = settings_reader.get_scenario()
            self.update_report_locations(scenario)

            delivered_messages_reader = DeliveredMessagesReportReader(self.delivered_messages_report)
            created_messages_reader = CreatedMessagesReportReader(self.created_messages_report)
            message_duplicates_reader = MessageDuplicatesReportReader(self.message_duplicates_report)
            node_location_reader = NodeLocationReader(settings_reader)
            message_processing_reader = MessageProcessingReportReader(self.message_processing_report)

            NodeLoad(message_processing_reader).load_distribution_by_hostgroup(self.output, scenario)
            DistanceDeliverytimeGraph(delivered_messages_reader,node_location_reader).create_scatter_plot(self.output, scenario)
            DeliveryCumulationGraph(delivered_messages_reader, created_messages_reader, settings_reader).create_all_from_scenario(self.output, scenario)
            hops = HopDistribution(delivered_messages_reader, created_messages_reader)
            hops.create_histogram_from_scenario(self.output, scenario)
            hops.create_boxplots_from_scenario(self.output, scenario)

            duplicates = Duplicates(message_duplicates_reader, created_messages_reader)
            for prefix in self.args.heatmap:
                duplicates.duplicates_heatmap(self.output, scenario, prefix)

    def update_report_locations(self, scenario):
        # DeliveredMessagesReport
        self.delivered_messages_report = os.path.join(self.args.reports,
                                                      scenario + "_DeliveredMessagesReport.txt")
        if not os.path.isfile(self.delivered_messages_report):
            raise ValueError(
                "DeliveredMessagesReport is not available at {}".format(self.delivered_messages_report))
        # CreatedMessagesReport
        self.created_messages_report = os.path.join(self.args.reports,
                                                    scenario + "_CreatedMessagesReport.txt")
        if not os.path.isfile(self.created_messages_report):
            raise ValueError("CreatedMessagesReport is not available at {}".format(self.created_messages_report))

        # MessageDuplicatesReport
        self.message_duplicates_report = os.path.join(self.args.reports,
                                                      scenario + "_MessageDuplicatesReport.txt")
        if not os.path.isfile(self.message_duplicates_report):
            raise ValueError(
                "MessageDuplicatesReport is not available at {}".format(self.message_duplicates_report))

        # MessageProcessingReport
        self.message_processing_report = os.path.join(self.args.reports,
                                                      scenario + "_MessageProcessingReport.txt")
        if not os.path.isfile(self.message_processing_report):
            raise ValueError(
                "MessageDuplicatesReport is not available at {}".format(self.message_processing_report))


if __name__ == "__main__":
    visualizer = Visualizer()
    visualizer.run()
