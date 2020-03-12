import datetime
import os
import pygtfs
from decimal import *

from src.elements.node import Node
from src.elements.node_list import NodeList
from src.elements.section import Section
from src.elements.trip import Trip


class GTFSParser:

    gtfs_files = [
        'agency.txt',
        'calendar_dates.txt',
        'feed_info.txt',
        'routes.txt',
        'stop_times.txt',
        'stops.txt',
        'trips.txt'
    ]
    gtfs_db_file = 'gtfs.sqlite'

    def __init__(self, gtfs_path, begin, end):

        if os.path.isdir(gtfs_path):
            self.schedule = self.create_db(gtfs_path)
        else:
            if not os.path.isfile(gtfs_path) or os.path.splitext(gtfs_path)[1] != '.sqlite':
                raise ValueError("{} is not a file".format(gtfs_path))
            self.schedule = pygtfs.Schedule(gtfs_path)

        first_date = self.schedule.feed_infos[0].feed_start_date
        last_date = self.schedule.feed_infos[0].feed_end_date
        if begin and end:
            self.start_date = datetime.datetime.strptime(begin, '%d.%m.%Y').date()
            if self.start_date < first_date:
                self.start_date = first_date
            self.end_date = datetime.datetime.strptime(end, '%d.%m.%Y').date()
            if self.end_date > last_date:
                self.end_date = last_date
        else:
            self.start_date = first_date + datetime.timedelta(
                days=(-first_date.weekday()) % 7)  # first monday after first_date
            self.end_date = self.start_date + datetime.timedelta(days=6)  # 1 week simulation

        print("Simulating from {} to {}. (Simulatable timeframe: {}-{})".format(
            self.start_date.strftime('%d.%m.%Y'),
            self.end_date.strftime('%d.%m.%Y'),
            self.schedule.feed_infos[0].feed_start_date.strftime('%d.%m.%Y'),
            self.schedule.feed_infos[0].feed_end_date.strftime('%d.%m.%Y')
        ))

        self.active_services_ids = {service.id for service in self.schedule.service_exceptions if
                                    self.start_date <= service.date <= self.end_date}

    def get_stops(self):
        """
        This function returns stop elements from the gtfs data.
        :return: dict {id: Node()}
        # todo just return a list of Nodes
        """
        stops = {}

        precision = Decimal('1.000000')
        for stop in self.schedule.stops:
            node = Node.from_gtfs(stop.stop_id, Decimal(stop.stop_lat).quantize(precision), Decimal(stop.stop_lon).quantize(precision))
            node = NodeList().add_node(node)
            stops[stop.stop_id] = node

        return stops

    def get_start_date(self):
        return datetime.datetime(self.start_date.year, self.start_date.month, self.start_date.day)

    def get_sections(self):
        """
        Return a set of all pairs of consecutive stops.
        The order of stops doesn't matter (a->b == b->a).
        :return: Set of Section() elements
        """
        sections = set()
        active_trips = [x for x in self.schedule.trips if x.service_id in self.active_services_ids]
        for trip in active_trips:
            stops = []
            for stop_time in trip.stop_times:
                stop_id = stop_time.stop_id
                stops.append([stop_time.stop_sequence, stop_id])
            stops = sorted(stops, key=lambda x: (x[0]))
            for i in range(0, len(stops) - 1):
                first_stop = stops[i][1]
                second_stop = stops[i+1][1]
                sections.add(Section(first_stop, second_stop))
        return sections

    def get_trips(self):
        """
        Return a list of all Trips within the simulation time window sorted by their first departure
        :return:
        """

        current_date = self.start_date
        all_trips = []
        while current_date <= self.end_date:
            active_services_ids = {service.id for service in self.schedule.service_exceptions if current_date == service.date}
            trips = [x for x in self.schedule.trips if x.service_id in active_services_ids]
            trips_on_day = []
            for trip in trips:
                stop_times = sorted(trip.stop_times, key=lambda x: x.stop_sequence)
                event_list = []
                for stop in stop_times:
                    date_time = datetime.datetime(current_date.year, current_date.month, current_date.day)
                    arrival = date_time + stop.arrival_time
                    departure = date_time + stop.departure_time
                    event_list.append((arrival, stop.stop_id))
                    event_list.append((departure, stop.stop_id))
                trips_on_day.append(Trip(event_list, self.start_date))
            trips_on_day = sorted(trips_on_day)

            all_trips += trips_on_day
            current_date += datetime.timedelta(days=1)

        return all_trips

    @classmethod
    def update_db(cls, gtfs_path, file_dates):
        if os.path.isfile(gtfs_path):
            return cls.update_db_file(gtfs_path, file_dates)
        elif os.path.isdir(gtfs_path):
            if not os.path.isfile(os.path.join(gtfs_path, cls.gtfs_db_file)):
                return True
            for file in cls.gtfs_files:
                latest_file_date = os.stat(os.path.join(gtfs_path, file)).st_mtime
                file_date = file_dates.get(file, None)
                if not file_date or file_date < latest_file_date:
                    return True
            return cls.update_db_file(os.path.join(gtfs_path, cls.gtfs_db_file), file_dates)
        raise ValueError("GTFS path is not valid.")

    @classmethod
    def update_db_file(cls, gtfs_db_file, file_dates):
        latest_gtfs_date = os.stat(gtfs_db_file).st_mtime
        gtfs_date = file_dates.get(os.path.basename(gtfs_db_file), None)
        if gtfs_date and gtfs_date >= latest_gtfs_date:
            return False
        return True

    @classmethod
    def update_file_dates(cls, gtfs_path, file_dates):
        if os.path.isdir(gtfs_path):
            for file in cls.gtfs_files:
                file_dates[file] = os.stat(os.path.join(gtfs_path, file)).st_mtime
            file_dates[cls.gtfs_db_file] = os.stat(os.path.join(gtfs_path, cls.gtfs_db_file)).st_mtime
        elif os.path.isfile(gtfs_path):
            db_file = os.path.basename(gtfs_path)
            file_dates[db_file] = os.stat(gtfs_path).st_mtime

    def create_db(self, gtfs_path):
        database_path = os.path.join(gtfs_path, self.gtfs_db_file)
        if os.path.isfile(database_path):
            os.remove(database_path)
        schedule = pygtfs.Schedule(database_path)
        pygtfs.append_feed(schedule, gtfs_path)
        return schedule
