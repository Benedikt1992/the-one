from geopy import distance

from src.osm_parser import OSMParser
from src.gtfs_parser import GTFSParser


class OSMStationExtender:
    def __init__(self):
        self.nodes = OSMParser("data/example.osm").get_nodes([('railway', 'stop'), ('railway', 'halt'), ('railway', 'station')])
        self.stops = GTFSParser("data/gtfs.sqlite").get_stops()

    def find_correlations(self):
        stop_node_correlations = {}
        for stop in self.stops:
            for node in self.nodes:
                if stop not in stop_node_correlations:
                    stop_node_correlations[stop] = [node]
                    continue
                # TODO replace 1000 with an configurable parameter (plausible inter-node distance)
                if distance.distance(self.nodes[node], self.nodes[stop_node_correlations[stop][0]]).meters < 1000:
                    stop_node_correlations[stop].append(node)
                    continue
                average_distance = self._distance_average(stop, stop_node_correlations[stop])
                node_distance = distance.distance(self.stops[stop], self.nodes[node]).meters
                if node_distance < average_distance:
                    stop_node_correlations[stop] = [node]
            #TODO delete assigned nodes from the global list
        return stop_node_correlations

    def _distance_average(self, stop, node_list):
        sum = 0
        for node in node_list:
            sum += distance.distance(self.stops[stop], self.nodes[node]).meters
        return sum / len(node_list)


if __name__ == "__main__":
    OSMStationExtender().find_correlations()
