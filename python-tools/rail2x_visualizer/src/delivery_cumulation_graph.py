import os

import matplotlib.pyplot as plt

from src.reader.created_messages_report_reader import CreatedMessagesReportReader
from src.reader.delivered_messages_report_reader import DeliveredMessagesReportReader
from src.reader.one_settings_reader import ONESettingsReader


class DeliveryCumulationGraph:
    """
    Show the cumulation of packages over time at the destination of the packages.
    Each destinantion is plotted in its own graph.
    """
    def __init__(self, delivery_reader: DeliveredMessagesReportReader, creation_reader: CreatedMessagesReportReader, settings: ONESettingsReader):
        self.delivered_messages_reader = delivery_reader
        self.created_messages_reader = creation_reader
        self.settings = settings
        self.threshold_toplist = {}
        self.area_toplist = {}

    def list_missing_messages(self, output_path, scenario):
        messages = self.created_messages_reader.get_messages()
        message_origin_map = self.created_messages_reader.get_message_origins()
        delivered_messages = self.delivered_messages_reader.get_delivered_messages()

        missing_msg = messages - delivered_messages
        with open(os.path.join(output_path, scenario + '_missing-messages.txt'), 'w') as file:
            file.write("# Missing {} messages out of {}:\n".format(len(missing_msg), len(messages)))
            for message in missing_msg:
                file.write("{} {}\n".format(message, message_origin_map[message]))

    def create_all_from_scenario(self, output_path, scenario):
        destinations = self.created_messages_reader.get_messages_grouped_by_destination()
        for destination, messages in destinations.items():
            self.create_station_from_scenario(output_path, scenario, destination, messages)
        self.__store_toplist(output_path, scenario)

    def create_station_from_scenario(self, output_path, scenario, destination, messages):
        array = self.__get_cumulation_array(destination)
        maximum = len(messages)

        area = 0
        for i in range(0, len(array)):
            # TODO make threashold configurable
            area += maximum - array[i]
            if array[i] > 0.85 * maximum:
                self.__add_to_toplist(destination, i, 'threshold')
        self.__add_to_toplist(destination, area, 'area')

        x_axis = [0] * len(array)
        for i in range(len(array)):
            x_axis[i] = i / (60.0)
        plt.plot(x_axis, array)

        plt.title('collected station data at {}'.format(destination))
        plt.ylabel('Messages')
        plt.xlabel('hours')
        axes = plt.gca()
        axes.set_ylim([0, maximum * 1.01])
        axes.set_yticks(list(plt.yticks()[0]) + [maximum])

        axes.text(0, maximum, '< max', verticalalignment='center', horizontalalignment='left')
        self.__store_figure(output_path, scenario, destination)

    def __get_cumulation_array(self, destination):
        deliveries = self.delivered_messages_reader.get_deliveries(destination)
        deliveries = sorted(deliveries, key=lambda x: x[0])
        max_time = self.settings.get_simulation_duration()
        cumulative_array = [0] * max_time
        cumulation = 0
        for delivery in deliveries:
            cumulation += 1
            cumulative_array[int(delivery[0])] = cumulation

        cumulation = cumulative_array[0]
        for i in range(1, max_time):
            if cumulative_array[i] > cumulative_array[i-1]:
                cumulation = cumulative_array[i]
            cumulative_array[i] = cumulation

        return cumulative_array

    @staticmethod
    def __store_figure(output, scenario, destination, format='svg'):
        outputpath = os.path.join(output, scenario + "_delivery-cumulation_" + destination + '.' + format)
        plt.savefig(outputpath, format=format)
        plt.clf()  # clear plot window
        plt.close('all')

    def __add_to_toplist(self, station, value, type):
        if type == 'threshold':
            self.threshold_toplist[station] = value
        elif type == 'area':
            self.area_toplist[station] = value

    def __store_toplist(self, output, scenario):
        outputpath = os.path.join(output, scenario + "_delivery-toplist-threshold.txt")
        with open(outputpath, "w") as file:
            for key, value in self.threshold_toplist.items():
                file.write("{},{}\n".format(key, str(value)))

        outputpath = os.path.join(output, scenario + "_delivery-toplist-area.txt")
        with open(outputpath, "w") as file:
            for key, value in self.area_toplist.items():
                file.write("{},{}\n".format(key, str(value)))



