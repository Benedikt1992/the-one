import simplejson as json


class Node:
    def __init__(self, osm_id=None, gtfs_id=None, lat=None, lon=None, x=None, y=None):
        self.osm_id = osm_id
        self.gtfs_id = gtfs_id
        self.lat = lat
        self.lon = lon
        self.x = x
        self.y = y
        self.stop_positions = [] # osm ids
        self.station = None # gtfs id

    @classmethod
    def from_osm(cls, osm_id, lat, lon):
        return cls(osm_id=osm_id, lat=lat, lon=lon)

    @classmethod
    def from_gtfs(cls, gtfs_id, lat, lon):
        return cls(gtfs_id=gtfs_id, lat=lat, lon=lon)

    def update_wkt(self, x, y):
        self.x = x
        self.y = y

    def get_gps(self):
        return self.lat, self.lon

    def add_stop_position(self, stop_id):
        self.stop_positions.append(stop_id)

    def add_station(self, station_id):
        if self.station:
            if self.station != station_id:
                raise ValueError("The station of a stop cannot be replaced. Old station: {}, New station: {}".format(str(self.station), str(station_id)))
        else:
            self.station = station_id

    def is_station(self):
        if self.stop_positions:
            return True
        return False

    def is_stop_position(self):
        if self.station:
            return True
        return False

    def __eq__(self, other):
        return (
            self.__class__ == other.__class__ and
            (
                self.osm_id == other.osm_id or
                self.gtfs_id == other.gtfs_id or
                (self.lat == other.lat and self.lon == other.lon) or
                (self.x == other.x and self.y == other.y)
            )
        )

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        if not self.osm_id:
            raise RuntimeError("Node not yet hashable. Node has no OSM ID.")
        return hash(self.osm_id)


class NodeSerializer(json.JSONEncoder):
    def default(self, o):
        return o.__dict__


def node_from_json(obj):
    node = Node()
    node.__dict__ = obj
    return node
