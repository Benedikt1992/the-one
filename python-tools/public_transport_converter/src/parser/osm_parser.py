import os
import xml.etree.ElementTree as ET
from decimal import *

from src.elements.node import Node
from src.elements.node_list import NodeList


class OSMParser:
    def __init__(self, osm_path):
        if not os.path.isfile(osm_path):
            raise ValueError("{} is not a file".format(osm_path))
        self.tree = ET.parse(osm_path)
        self.root = self.tree.getroot()
        self.last_id, self.minlat, self.minlon, self.maxlat, self.maxlon = self._find_next_id()

    def get_nodes(self, tags=None):
        """
        This function returns node elements from the osm data. Optionally with specific tags.
        If there are several tags it is sufficient that the element contains one of the tags.
        Each node is added to the global node list.
        :param tags: Array of tuples (key: value). Several entries are connected with logical or.
        :return: dict {id: Node()}
        # todo just return list of Node
        """
        nodelist = NodeList()
        nodes = {}
        for item in self.root.findall('node'):
            if tags is None:
                node = Node.from_osm(int(item.get('id')), Decimal(item.get('lat')), Decimal(item.get('lon')))
                node = nodelist.add_node(node)
                nodes[int(item.get('id'))] = node
            else:
                for k, v in tags:
                    query = "tag[@k='{}'][@v='{}']".format(k, v)
                    if item.find(query) is not None:
                        node = Node.from_osm(int(item.get('id')), Decimal(item.get('lat')), Decimal(item.get('lon')))
                        node = nodelist.add_node(node)
                        nodes[int(item.get('id'))] = node
                        break
        return nodes

    def get_ways(self):
        """
        Filter always of osm data.
        :return: list of waypoint lists.
        """
        ways = []
        for way in self.root.findall('way'):
            waypoints = []
            for point in way.findall('nd'):
                waypoints.append(int(point.get('ref')))
            ways.append(waypoints)
        return ways

    def add_node(self, node: Node):
        """
        Add a node to the OSM data. The function will provide a unique ID.
        :param node: Node object
        :return: OSM ID of the node element
        """

        if node.osm_id:
            return node.osm_id

        self.last_id += 1
        node_element = ET.SubElement(self.root, 'node', attrib={"id": str(self.last_id), "lat": str(node.lat), "lon": str(node.lon), "visible": "true"})
        ET.SubElement(node_element, 'tag', attrib={"k": "type", "v": "station"})
        
        if node.lat < self.minlat:
            self.minlat = node.lat
        elif node.lat > self.maxlat:
            self.maxlat = node.lat
            
        if node.lon < self.minlon:
            self.minlon = node.lon
        elif node.lon > self.maxlon:
            self.maxlon = node.lon

        node.osm_id = self.last_id
        return self.last_id

    def add_way(self, waypoints):
        """
        Add a way to the osm data.
        :param waypoints: list of IDs of OSM nodes
        :return: ID of the way element
        """
        for point in waypoints:
            if self.root.find("node[@id='{}']".format(point)) is None:
                raise AttributeError("Waypoint is not part of the OSM data")
        self.last_id += 1
        way = ET.SubElement(self.root, 'way', attrib={"id": str(self.last_id)})
        for point in waypoints:
            ET.SubElement(way, 'nd', attrib={"ref": str(point)})
        ET.SubElement(way, 'tag', attrib={"k": "type", "v": "connector"})
        return self.last_id

    def store(self, path):
        """
        Store the data as OSM XML. Add bounds of the file if missing.
        :param path: File path
        """
        if self.root.find("bounds") is None:
            ET.SubElement(self.root, 'bounds',
                          attrib={"minlat": str(self.minlat), "minlon": str(self.minlon), "maxlat": str(self.maxlat), "maxlon": str(self.maxlon)})

        with open(path, "w", encoding='utf-8') as file:
            sort_order = {
                'note': 0,
                'meta': 1,
                'bounds': 2,
                'node': 3,
                'way': 4,
                'relation': 5
            }
            self.root[:] = sorted(self.root, key=lambda elem: sort_order[elem.tag])
            string = ET.tostring(self.root, encoding='unicode')
            file.write(string)

    def _find_next_id(self):
        last_id = 0
        # latitude is horizontal from -90 -- 0 -- 90
        # longitude is vertical from -180 -- 0 -- 180
        minlat = Decimal('90')
        minlon = Decimal('180')
        maxlat = Decimal('-90')
        maxlon = Decimal('-180')

        for item in self.root:
            if 'id' in item.attrib:
                if int(item.attrib['id']) > last_id:
                    last_id = int(item.attrib['id'])
            if 'lat' in item.attrib:
                if Decimal(item.attrib['lat']) > maxlat:
                    maxlat = Decimal(item.attrib['lat'])
                if Decimal(item.attrib['lat']) < minlat:
                    minlat = Decimal(item.attrib['lat'])
            if 'lon' in item.attrib:
                if Decimal(item.attrib['lon']) > maxlon:
                    maxlon = Decimal(item.attrib['lon'])
                if Decimal(item.attrib['lon']) < minlon:
                    minlon = Decimal(item.attrib['lon'])

        return last_id, minlat, minlon, maxlat, maxlon

    def get_bounds(self):
        return self.minlat, self.minlon, self.maxlat, self.maxlon
