class Trip:
    def __init__(self, events):
        """
        :param events: List of (absolute time, gtfs station id)
        """
        self.event_list = events

    def __lt__(self, other):
        return self.event_list[0][0] < other.event_list[0][0]
