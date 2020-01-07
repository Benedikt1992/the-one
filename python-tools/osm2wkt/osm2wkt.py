import xml.etree.ElementTree as ET
from decimal import *
from geopy import distance
import networkx as nx
# TODO include libs as requirements in setup.py

tree = ET.parse("ICE-germany-extended.osm")
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
    if nodes.get(node.get('id'), None) is not None:
        raise ArithmeticError("Duplicate ID")
    lat = Decimal(node.get('lat'))
    lon = Decimal(node.get('lon'))
    x = distance.distance((lat, lon),
                          (lat, minlon)).meters
    y = distance.distance((lat, lon),
                          (minlat, lon)).meters
    nodes[node.get('id')] = (lat, lon, x, y)

ways = []
for way in root.findall('way'):
    waypoints = []
    for point in way.findall('nd'):
        waypoints.append(point.get('ref'))
    ways.append(waypoints)

graph = nx.Graph()
for way in ways:
    for i in range(len(way)):
        if i == 0:
            continue
        graph.add_edge(way[i-1], way[i])

connected_sets = list(nx.connected_components(graph))
largest_set = max(connected_sets, key=len)
connected_sets.remove(largest_set)
for partition in connected_sets:
    r = largest_set.intersection(partition)
    if r:
        raise ArithmeticError("Partitions are not disjoint")

for partition in connected_sets:
    for way in ways:
        if partition.intersection(way):
            ways.remove(way)

print("Removed {} unconnected nodes from {} nodes in total.".format(graph.number_of_nodes()-len(largest_set), graph.number_of_nodes()))

graph = nx.Graph()
for way in ways:
    for i in range(len(way)):
        if i == 0:
            continue
        graph.add_edge(way[i-1], way[i])
connected_sets = list(nx.connected_components(graph))
if len(connected_sets) > 1:
    print("Couldn't remove all partitions.")

# TODO test if gtfs stations are still part of the graph
# todo have a look into some partitions. Why are they not connected?

with open('ICE-germany-extended.wkt', 'w') as output:
    for way in ways:
        output.write("LINESTRING (")
        waypoints = []
        for point in way:
            node = nodes[point]
            waypoints.append("{} {}".format(node[2], node[3]))

        output.write(", ".join(waypoints))
        output.write(")\n")
