import os

from src.singleton import Singleton


class Statistics(metaclass=Singleton):
    def __init__(self):
        self.area_metrics = []
        self.messages = 0
        self.delivered_messages = 0
        self.hops_min = 0
        self.hops_max = 0
        self.hops_mode = 0
        self.hops_avg = 0
        self.incoming_avg = 0
        self.outgoing_avg = 0

    def add_area_metric(self, destination, area):
        self.area_metrics.append((destination, area))

    def add_delivery_summary(self, messages, delivered_messages):
        self.messages = messages
        self.delivered_messages = delivered_messages

    def set_hop_stats(self, minimum, maximum, mode, avg):
        self.hops_min = minimum
        self.hops_max = maximum
        self.hops_mode = mode
        self.hops_avg = avg

    def set_processing_stats(self, incoming_avg, outgoing_avg):
        self.incoming_avg = incoming_avg
        self.outgoing_avg = outgoing_avg

    def print_and_clear_stats(self, out_dir, scenario):
        with open(os.path.join(out_dir, scenario + "_statistics.txt"), 'w') as file:
            for dst, metric in self.area_metrics:
                file.write("Delivery metric (area) for {}: {}\n".format(dst, metric))

            file.write("Messages delivered: {}/{}\n".format(self.delivered_messages, self.messages))
            file.write("Hop stats (min, max, mode, avg): {}, {}, {}, {:.2f}\n".format(self.hops_min,
                                                                                  self.hops_max,
                                                                                  self.hops_mode,
                                                                                  self.hops_avg))
            file.write("Tranceiver stats (in, out): {:.2f}, {:.2f}\n".format(self.incoming_avg, self.outgoing_avg))
        self.__init__()



