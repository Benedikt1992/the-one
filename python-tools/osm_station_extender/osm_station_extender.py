from geopy import distance

from src.osm_parser import OSMParser
from src.gtfs_parser import GTFSParser


class OSMStationExtender:
    def __init__(self):
        self.osm_parser = OSMParser("data/example.osm")
        self.gtfs_parser = GTFSParser("data/gtfs.sqlite")
        self.nodes = self.osm_parser.get_nodes([('railway', 'stop'), ('railway', 'halt'), ('railway', 'station')])
        self.stops = self.gtfs_parser.get_stops()

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
            # TODO uncomment node deletion
            for node in stop_node_correlations[stop]:
                self.nodes.pop(node, None)
            if not self.nodes:
                raise RuntimeError("Not enough nodes in osm data.")
        return stop_node_correlations

    def _distance_average(self, stop, node_list):
        sum = 0
        for node in node_list:
            sum += distance.distance(self.stops[stop], self.nodes[node]).meters
        return sum / len(node_list)

    def store_stops_as_nodes(self, correlations):
        new_correlations = {}
        for stop in correlations:
            new_id = self.osm_parser.add_node(self.stops[stop])
            content = self.stops.pop(stop, None)
            self.stops[new_id] = content
            new_correlations[new_id] = correlations[stop]
        return new_correlations

    def store_ways(self, correlations):
        for stop in correlations:
            for node in correlations[stop]:
                self.osm_parser.add_way([stop, node])

    def store_osm(self):
        self.osm_parser.store("data/example-extended.osm")


if __name__ == "__main__":
    extender = OSMStationExtender()
    correlations = extender.find_correlations()
    correlations = extender.store_stops_as_nodes(correlations)
    extender.store_ways(correlations)
    extender.store_osm()

