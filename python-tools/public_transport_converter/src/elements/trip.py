class Trip:
    def __init__(self, events):
        """
        :param events: List of (absolute time, gtfs station id)
        """
        self.event_list = events

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
