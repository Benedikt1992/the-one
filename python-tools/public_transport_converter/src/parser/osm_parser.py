import os
import xml.etree.ElementTree as ET
from decimal import *


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
        :param tags: Array of tuples (key: value). Several entries are connected with logical or.
        :return: dict {id: (lat, lon)}
        """
        nodes = {}
        for item in self.root.findall('node'):
            if tags is None:
                nodes[int(item.get('id'))] = (Decimal(item.get('lat')), Decimal(item.get('lon')))
            else:
                for k, v in tags:
                    query = "tag[@k='{}'][@v='{}']".format(k, v)
                    if item.find(query) is not None:
                        nodes[int(item.get('id'))] = (Decimal(item.get('lat')), Decimal(item.get('lon')))
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
                waypoints.append(Decimal(point.get('ref')))
            ways.append(waypoints)
        return ways

    def add_node(self, coordinates):
        """
        Add a node to the OSM data. The function will provide a unique ID.
        :param coordinates: GPS coordinates in decimal form in a tuple '(lat, lon)'
        :return: OSM ID of the node element
        """
        self.last_id += 1
        node = ET.SubElement(self.root, 'node', attrib={"id": str(self.last_id), "lat": str(coordinates[0]), "lon": str(coordinates[1]), "visible": "true"})
        ET.SubElement(node, 'tag', attrib={"k": "type", "v": "station"})
        
        if coordinates[0] < self.minlat:
            self.minlat = coordinates[0]
        elif coordinates[0] > self.maxlat:
            self.maxlat = coordinates[0]
            
        if coordinates[1] < self.minlon:
            self.minlon = coordinates[1]
        elif coordinates[1] > self.maxlon:
            self.maxlon = coordinates[1]

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
