import re

from src.reader.one_settings_reader import ONESettingsReader


class NodeLocationReader:
    def __init__(self, settings: ONESettingsReader):
        nodes = settings.get_stationary_nodes()

        self.node_dict = {}
        for name, config in nodes.items():
            with open(config['location'], 'r') as file:
                for node_id, line in enumerate(file, config['start']):
                    regex = r'POINT \((?P<x>[0-9.]+) (?P<y>[0-9.]+)\)'
                    matches = re.search(regex, line)
                    node_name = name + str(node_id)
                    x = float(matches.group('x'))
                    y = float(matches.group('y'))
                    self.node_dict[node_name] = (x, y)

    def get_node_location(self, node):
        return self.node_dict.get(node, None)
