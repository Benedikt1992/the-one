import os
from argparse import ArgumentParser, ArgumentError

from src.delivery_cumulation_graph import DeliveryCumulationGraph
from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader


class Visualizer:
    def __init__(self):
        # TODO enable to auto process the results of a batched simulation run
        parser = ArgumentParser(description='Extend osm xml data by gtfs stations and connect them to their stop points')
        parser.add_argument('-r', '--reports', required=True, help="Directory of the generated reports.")
        parser.add_argument('-s', '--scenario', required=True, help='Name of the scenario which should be visualized (without the report names).')
        parser.add_argument('-o', '--output', nargs='?', default = '', help = "Output directory. The directory of the reports is used by default.")

        self.args = parser.parse_args()

        if not os.path.isdir(self.args.reports):
            raise ArgumentError("-r/--reports needs to be a directory.")

        if not self.args.output:
            self.output = self.args.reports
        else:
            if not os.path.isdir(self.args.output):
                os.makedirs(self.args.output)
            self.output = self.args.output

        # DeliveredMessagesReport
        self.delivered_messages_report = os.path.join(self.args.reports, self.args.scenario + "_DeliveredMessagesReport.txt")
        if not os.path.isfile(self.delivered_messages_report):
            raise ValueError("DeliveredMessagesReport is not available at {}".format(self.delivered_messages_report))

        # CreatedMessagesReport
        self.created_messages_report = os.path.join(self.args.reports, self.args.scenario + "_CreatedMessagesReport.txt")
        if not os.path.isfile(self.created_messages_report):
            raise ValueError("CreatedMessagesReport is not available at {}".format(self.created_messages_report))
        # todo test if all reports are available

    def run(self):
        delivered_messages_reader = DeliveredMessagesReportReader(self.delivered_messages_report)
        created_messages_reader = CreatedMessagesReportReader(self.created_messages_report)

        DeliveryCumulationGraph(delivered_messages_reader, created_messages_reader).create_all_from_scenario(self.output, self.args.scenario)
        # TODO implement the different graphs from BP
        pass


if __name__ == "__main__":
    visualizer = Visualizer()
    visualizer.run()
