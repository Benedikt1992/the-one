import datetime

from src.elements.node_list import NodeList


class Trip:
    def __init__(self, events, zero_time):
        """
        :param events: List of (absolute time, gtfs station id)
        """
        self.event_list = events
        self.zero_time = datetime.datetime(zero_time.year, zero_time.month, zero_time.day)

    def end(self):
        return self.event_list[-1][0]

    def start(self):
        return self.event_list[0][0]

    def first_stop(self):
        return self.event_list[0][1]

    def last_stop(self):
        return self.event_list[-1][1]

    def __lt__(self, other):
        return self.event_list[0][0] < other.event_list[0][0]

    def __str__(self):
        string_elements = []
        nodes = NodeList()
        for event in self.event_list:
            timestamp = (event[0] - self.zero_time).total_seconds() / 60  # User Minutes as simulation step
            node = nodes.find_by_gtfs_id(event[1])
            string_elements.append("{} {:.6f} {:.6f}".format(str(int(timestamp)), node.x, node.y))
        return ", ".join(string_elements)

