from geopy import distance


class OSMExtender:
    def __init__(self, osm_parser):
        self.osm_parser = osm_parser

    def extend_with_gtfs_station(self, gtfs_parser, osm_filter, distance_threshold):
        """
        Extend existing OSM data with nodes for stations of a gtfs schedule.
        Each station is connected with the corresponding stop positions of each platform.
        :param gtfs_parser: gtfs parser object
        :param osm_filter: key value pairs used to filter stop positions from the osm data
        :param distance_threshold: maximum distance allowed between station and its stops in meters
        :return [new ids of gtfs stations]
        """
        nodes, stops = self._filter_data(osm_filter, gtfs_parser)
        stop_node_correlations = self._correlate_points(nodes, stops, distance_threshold)
        stop_node_correlations = self._store_stops_as_nodes(stop_node_correlations, stops)
        self._store_ways(stop_node_correlations)
        return stop_node_correlations.keys()

    def _filter_data(self, filter, gtfs_parser):
        nodes = self.osm_parser.get_nodes(filter)
        stops = gtfs_parser.get_stops()

        return nodes, stops

    def _correlate_points(self, nodes, stops, distance_threshold):
        """
        Find correlating OSM nodes for each GTFS station based on a distance threshold.
        The threshold defines the maximum plausible distance between a GTFS station and correlating OSM stops
        :return: {<GTFS station id>: [<OSM Node ID>]}
        """
        stop_node_correlations = {}
        for stop in stops:
            for node in nodes:
                if stop not in stop_node_correlations:
                    stop_node_correlations[stop] = [node]
                    continue
                if distance.distance(nodes[node].get_gps(),
                                     nodes[stop_node_correlations[stop][0]].get_gps()).meters < distance_threshold:
                    stop_node_correlations[stop].append(node)
                    continue
                average_distance = self._distance_average(stops[stop], stop_node_correlations[stop], nodes)
                node_distance = distance.distance(stops[stop].get_gps(), nodes[node].get_gps()).meters
                if node_distance < average_distance:
                    stop_node_correlations[stop] = [node]

            for node in stop_node_correlations[stop]:
                stops[stop].add_stop_position(node)
                nodes[node].add_station(stop)
                nodes.pop(node, None)
            if not nodes:
                raise RuntimeError("Not enough nodes in osm data.")
        return stop_node_correlations

    def _distance_average(self, stop, node_list, nodes):
        sum = 0
        for node in node_list:
            sum += distance.distance(stop.get_gps(), nodes[node].get_gps()).meters
        return sum / len(node_list)

    def _store_stops_as_nodes(self, correlations, stops):
        """
        Store GTFS stations within the OSM XML data.
        :param correlations: {<GTFS station id>: [<OSM Node ID>]}
        :return: updated correlations with the new OSM ID for stations
        """
        new_correlations = {}
        for stop in correlations:
            new_id = self.osm_parser.add_node(stops[stop])
            content = stops.pop(stop, None)
            stops[new_id] = content
            new_correlations[new_id] = correlations[stop]
        return new_correlations

    def _store_ways(self, correlations):
        """
        The the connections between GTFS station and each stop as straight lines.
        :param correlations: {<GTFS station id>: [<OSM Node ID>]}
        """
        for stop in correlations:
            for node in correlations[stop]:
                self.osm_parser.add_way([stop, node])
