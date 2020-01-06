import xml.etree.ElementTree as ET
from decimal import *
from geopy import distance


tree = ET.parse("export.osm")
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
# todo use multiprocessing

# nodes = {id: (lat,lon,x,y)}
nodes = {}
for node in root.findall('node'):
    nodes[node.get('id')] = (Decimal(node.get('lat')), Decimal(node.get('lon')), None, None)

with open('export.wkt', 'w') as output:
    for way in root.findall('way'):
        output.write("LINESTRING (")
        waypoints = []
        for point in way.findall('nd'):
            node = nodes[point.get('ref')]
            if node[2] is None and node[3] is None:
                x = distance.distance((node[0], node[1]),
                                      (node[0], minlon)).meters
                y = distance.distance((node[0], node[1]),
                                      (minlat, node[1])).meters
                nodes[point.get('ref')] = (node[0], node[1], x, y)
            waypoints.append("{} {}".format(x, y))
        output.write(", ".join(waypoints))
        output.write(")\n")
