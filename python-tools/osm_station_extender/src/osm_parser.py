import os
import xml.etree.ElementTree as ET


class OSMParser:
    def __init__(self, osm_path):
        if not os.path.isfile(osm_path):
            raise ValueError("{} is not a file".format(osm_path))
        self.root = ET.parse(osm_path).getroot()
        self.next_id = self.find_next_id()

    def get_nodes(self, tags=None):
        ET.SubElement(self.root, 'node', attrib={"id":"298884269", "lat":"54.0901746", "lon":"12.2482632"})
        print(ET.tostring(self.root))

    def add_node(self):
        raise NotImplementedError
        # TODO
        ET.SubElement(self.root, 'node', attrib={"id": "298884269", "lat": "54.0901746", "lon": "12.2482632"})

    def save_to_file(self):
        raise NotImplementedError
        # TODO Sort output according to https://wiki.openstreetmap.org/wiki/OSM_XML
        ET.tostring(self.root)

    def find_next_id(self):
        # TODO find the highest id that is not yet used for new elements
        raise NotImplementedError
