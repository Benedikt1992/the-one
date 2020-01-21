class Node:
    def __init__(self, osm_id=None, gtfs_id=None, lat=None, lon=None, x=None, y=None):
        self.osm_id = osm_id
        self.gtfs_id = gtfs_id
        self.lat = lat
        self.lon = lon
        self.x = x
        self.y = y

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