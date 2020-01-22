import os
import simplejson as json

from src.elements.node import Node, NodeSerializer, node_from_json


class NodeList:
    __instance = None
    nodes = []
    osm_index = {}
    gtfs_index = {}

    def __new__(cls):
        if not NodeList.__instance:
            NodeList.__instance = object.__new__(cls)
        return NodeList.__instance

    @classmethod
    def store_to_cache(cls, cache_dir, project_name):
        if not cls.__instance:
            return
        with open(os.path.join(cache_dir, project_name + '_nodelist.json'), 'w') as file:
            json.dump(cls.nodes, file, cls=NodeSerializer)

    @classmethod
    def load_from_cache(cls, cache_dir, project_name):
        with open(os.path.join(cache_dir, project_name + '_nodelist.json'), 'r') as file:
            nodes = json.load(file, object_hook=node_from_json, use_decimal=True)
        cls.nodes = nodes
        for node in nodes:
            if node.osm_id:
                cls.osm_index[node.osm_id] = node
            if node.gtfs_id:
                cls.gtfs_index[node.gtfs_id] = node

    def add_node(self, node: Node):
        """
        Add a node or return the existing node.
        :param node:
        :return:
        """
        existing_node = self.osm_index.get(node.osm_id, False) or self.gtfs_index.get(node.gtfs_id, False)
        if existing_node:
            return existing_node
        self.nodes.append(node)
        if node.gtfs_id:
            self.gtfs_index[node.gtfs_id] = node
        if node.osm_id:
            self.osm_index[node.osm_id] = node
        return node

    def find_by_osm_id(self, identifier):
        node = self.osm_index.get(identifier, None)
        if not node:
            nodelist = [node for node in self.nodes if node.osm_id == identifier]
            if len(nodelist) > 1:
                raise RuntimeError("Multiple nodes with same OSM id: {}".format(str(node)))
            if not nodelist:
                node = None
            else:
                node = nodelist[0]
        return node

    def find_by_gtfs_id(self, identifier):
        node = self.gtfs_index.get(identifier, None)
        if not node:
            nodelist = [node for node in self.nodes if node.gtfs_id == identifier]
            if len(nodelist) > 1:
                raise RuntimeError("Multiple nodes with same GTFS id: {}".format(str(node)))
            if not nodelist:
                node = None
            else:
                node = nodelist[0]
        return node

    def __iter__(self):
        return self.nodes.__iter__()

