import xml.etree.ElementTree as ET
from decimal import *
from geopy import distance


tree = ET.parse("test.osm")
root = tree.getroot()

# latitude is horizontal from -90 -- 0 -- 90
# longitude is vertical from -180 -- 0 -- 180
minlat = Decimal('90')
minlon = Decimal('180')
maxlat = Decimal('-90')
maxlon = Decimal('-180')

bounds = root.find('bounds')
if bounds is not None:
    minlat = Decimal(bounds.get('minlat'))
    minlon = Decimal(bounds.get('minlon'))
    maxlat = Decimal(bounds.get('maxlat'))
    maxlon = Decimal(bounds.get('maxlon'))
else:
    for item in root.findall('node'):
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

with open('test.wkt', 'w') as output:
    node_cache = {}
    for way in root.findall('way'):
        output.write("LINESTRING (")
        waypoints = []
        for point in way.findall('nd'):
            point.get('ref')
            node = root.find("node[@id='{}']".format(point.get('ref')))
            # todo use node_cache
            if node is None:
                raise ArithmeticError("Node {} not found".format(point.get('ref')))
            x = distance.distance((Decimal(node.get('lat')), Decimal(node.get('lon'))),
                                  (Decimal(node.get('lat')), minlon)).meters
            y = distance.distance((Decimal(node.get('lat')), Decimal(node.get('lon'))),
                                  (minlat, Decimal(node.get('lon')))).meters
            node_cache[node.get('id')] = (x, y)
            waypoints.append("{} {}".format(x, y))
        output.write(", ".join(waypoints))
        output.write(")\n")
