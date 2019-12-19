from src.osm_parser import OSMParser

if __name__ == "__main__":
    OSMParser("data/example.osm").collect_nodes()
