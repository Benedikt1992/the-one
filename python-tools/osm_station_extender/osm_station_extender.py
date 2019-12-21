from src.osm_parser import OSMParser
from src.gtfs_parser import GTFSParser

if __name__ == "__main__":
    # OSMParser("data/example.osm").get_nodes([('railway', 'stop'), ('railway', 'halt'), ('railway', 'station')])
    GTFSParser("data/gtfs.sqlite").get_stops()
