import os

from geopy import distance
from argparse import ArgumentParser, ArgumentTypeError

from src.osm_parser import OSMParser
from src.gtfs_parser import GTFSParser
from src.util.store_key_pair import StoreKeyPair


class PublicTransportConverter:
    def __init__(self):
        parser = ArgumentParser(description='Extend osm xml data by gtfs stations and connect them to their stop points')
        parser.add_argument('-osm', '--osm', required=True, help="Input OSM XML file")
        parser.add_argument('-gtfs', '--gtfs', required=True, help='Input GTFS sqlite database (see module pygtfs)')
        parser.add_argument('-f', '--filter', action=StoreKeyPair, nargs="?",
                            default=[('railway', 'stop'), ('railway', 'halt'), ('railway', 'station'), ('public_transport', 'stop_position')],
                            help="Add filter to search for nodes. Use list in form of KEY1=VAL1,KEY2=VAL2. Each entry is connected with logical or.")
        parser.add_argument('-d', '--distance', nargs='?', type=int, default=1000, help="Maximum distance between gtfs station and osm stop positions.")
        parser.add_argument('-o', '--output', nargs='?', default='', help="Output file. The source OSM File is extended with '-extended' by default.")
        args = parser.parse_args()

        self.osm_parser = OSMParser(args.osm)
        self.gtfs_parser = GTFSParser(args.gtfs)
        self.nodes = self.osm_parser.get_nodes(args.filter)
        self.stops = self.gtfs_parser.get_stops()
        self.distance = args.distance
        self.output = os.path.splitext(args.osm)[0] + '-extended' + os.path.splitext(args.osm)[1]

    def find_correlations(self):
        """
        Find correlating OSM nodes for each GTFS station based on a distance threshold.
        The threshold defines the maximum plausible distance between a GTFS station an correlating OSM stops
        :return: {<GTFS station id>: [<OSM Node IDs>]}
        """
        stop_node_correlations = {}
        for stop in self.stops:
            for node in self.nodes:
                if stop not in stop_node_correlations:
                    stop_node_correlations[stop] = [node]
                    continue
                if distance.distance(self.nodes[node], self.nodes[stop_node_correlations[stop][0]]).meters < self.distance:
                    stop_node_correlations[stop].append(node)
                    continue
                average_distance = self._distance_average(stop, stop_node_correlations[stop])
                node_distance = distance.distance(self.stops[stop], self.nodes[node]).meters
                if node_distance < average_distance:
                    stop_node_correlations[stop] = [node]
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
        """
        Store GTFS stations within the OSM XML data.
        :param correlations: {<GTFS station id>: [<OSM Node IDs>]}
        :return: updated correlations with the new OSM ID for stations
        """
        new_correlations = {}
        for stop in correlations:
            new_id = self.osm_parser.add_node(self.stops[stop])
            content = self.stops.pop(stop, None)
            self.stops[new_id] = content
            new_correlations[new_id] = correlations[stop]
        return new_correlations

    def store_ways(self, correlations):
        """
        The the connections between GTFS station and each stop as straight lines.
        :param correlations: {<GTFS station id>: [<OSM Node IDs>]}
        """
        for stop in correlations:
            for node in correlations[stop]:
                self.osm_parser.add_way([stop, node])

    def store_osm(self):
        """
        Store the OSM data as OSM XML.
        :return:
        """
        self.osm_parser.store(self.output)


if __name__ == "__main__":
    converter = PublicTransportConverter()
    correlations = converter.find_correlations()
    correlations = converter.store_stops_as_nodes(correlations)
    converter.store_ways(correlations)
    converter.store_osm()

