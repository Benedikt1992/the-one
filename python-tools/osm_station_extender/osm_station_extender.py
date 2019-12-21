from src.osm_parser import OSMParser

if __name__ == "__main__":
    OSMParser("data/example.osm").get_nodes([('railway', 'stop'), ('railway', 'halt'), ('railway', 'station')])
