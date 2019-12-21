import os
import xml.etree.ElementTree as ET
from decimal import *


class OSMParser:
    def __init__(self, osm_path):
        if not os.path.isfile(osm_path):
            raise ValueError("{} is not a file".format(osm_path))
        self.tree = ET.parse(osm_path)
        self.root = self.tree.getroot()
        self.next_id, self.minlat, self.minlon, self.maxlat, self.maxlon = self.find_next_id()

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
        # TODO test if results are correct
        return nodes

    def add_node(self):
        raise NotImplementedError
        # TODO
        ET.SubElement(self.root, 'node', attrib={"id": "298884269", "lat": "54.0901746", "lon": "12.2482632"})

    def save_to_file(self):
        raise NotImplementedError
        # TODO Sort output according to https://wiki.openstreetmap.org/wiki/OSM_XML
        ET.tostring(self.root)

    def find_next_id(self):
        next_id = 0
        minlat = 0
        minlon = 0
        maxlat = 0
        maxlon = 0

        for item in self.root:
            if 'id' in item.attrib:
                if int(item.attrib['id']) > next_id:
                    next_id = int(item.attrib['id'])
            if 'lat' in item.attrib:
                if Decimal(item.attrib['lat']) > maxlat:
                    maxlat = Decimal(item.attrib['lat'])
                elif Decimal(item.attrib['lat']) < minlat or minlat == 0:
                    minlat = Decimal(item.attrib['lat'])
            if 'lon' in item.attrib:
                if Decimal(item.attrib['lon']) > maxlon:
                    maxlon = Decimal(item.attrib['lon'])
                elif Decimal(item.attrib['lon']) < minlon or minlon == 0:
                    minlon = Decimal(item.attrib['lon'])

        return next_id, minlat, minlon, maxlat, maxlon
