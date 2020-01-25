import datetime
from src.elements.trip import Trip


class Route:
    """
    This describes a ROUTE within the MapScheduledMovement for the ONE simulator.
    """
    def __init__(self, pause_between_trips=datetime.timedelta(minutes=30)):
        self.trips = []
        self.pause_between_trips = pause_between_trips

    def __add__(self, other):
        if isinstance(other, Trip):
            self.trips.append(other)
            return self
        raise ValueError("'+' operator not implemented for objects of type {}".format(type(other)))

    def append(self, other):
        return self.__add__(other)

    def __gt__(self, other):
        print("gt")
        if not self.trips:
            raise ValueError("Can't compare emtpy route.")
        if isinstance(other, Trip):
            return self.trips[-1].end() + self.pause_between_trips > other.start()
        raise ValueError("'>' operator not implemented for type {}".format(type(other)))

    def __lt__(self, other):
        print("lt")
        if not self.trips:
            raise ValueError("Can't compare emtpy route.")
        if isinstance(other, Trip):
            return self.trips[-1].end() + self.pause_between_trips < other.start()
        raise ValueError("'<' operator not implemented for type {}".format(type(other)))
