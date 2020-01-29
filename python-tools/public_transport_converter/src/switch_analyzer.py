import os

import networkx as nx

from src.parser.osm_parser import OSMParser


class SwitchAnalyzer:
    def __init__(self, output, osm_parser: OSMParser):
        self.osm_parser = osm_parser
        self.output_dir = os.path.dirname(output)
        self.project_name = os.path.splitext(os.path.basename(output))[0]
        ways = self.osm_parser.get_ways()

        self.graph = nx.Graph()
        for way in ways:
            for i in range(len(way)):
                if i == 0:
                    continue
                self.graph.add_edge(way[i - 1], way[i])

    def analyze(self):
        self.analyze_switch_degrees()

    def analyze_switch_degrees(self):
        nodes = self.osm_parser.get_nodes([('railway', 'switch')]).keys()

        max_neighbor = 0
        neighbor_distribution = {}
        for node in nodes:
            dist = neighbor_distribution.get(len(self.graph[node]), 0) + 1
            neighbor_distribution[len(self.graph[node])] = dist
            if len(self.graph[node]) == 2:
                print(node)
            if len(self.graph[node]) == 1:
                print(node)
            if len(self.graph[node]) > max_neighbor:
                max_neighbor = len(self.graph[node])

        with open(os.path.join(self.output_dir, self.project_name + '-switch-degrees.csv'), 'w') as file:
            file.write("degree, amount\n")
            keys = sorted(neighbor_distribution.keys())
            for key in keys:
                file.write("{}, {}\n".format(key,neighbor_distribution[key]))

        print("Maximum number of neighbors for a switch point is {}".format(max_neighbor))
