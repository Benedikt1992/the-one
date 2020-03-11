import hashlib
import os
import pandas as pd

from geopy.geocoders import Nominatim
from geopy.extra.rate_limiter import RateLimiter
from functools import partial
from tqdm import tqdm
tqdm.pandas()


def _location_to_country(loc):
    try:
        return loc.raw['address']['country']
    except KeyError:
        print("This location has no associated country: " + str(loc))
        return ""


def _load_location_from_cache(source_dir):
    cache_path = os.path.join(source_dir, '.geolocator.cache')
    if not os.path.isdir(cache_path):
        return pd.DataFrame()

    stops_path = os.path.join(source_dir, 'stops.txt')
    stops_hash = _calc_hash(stops_path)

    with open(os.path.join(cache_path, "stops.hash"), 'r') as file:
        cache_hash = file.readline()
    if cache_hash == stops_hash:
        print("Using cached locations: " + os.path.join(cache_path, "annotated_stops.txt"))
        return pd.read_csv(os.path.join(cache_path, "annotated_stops.txt"))
    return pd.DataFrame()


def _store_location_to_cache(source_dir, annotated_stops: pd.DataFrame):
    cache_path = os.path.join(source_dir, '.geolocator.cache')
    if not os.path.isdir(cache_path):
        os.mkdir(cache_path)

    stops_path = os.path.join(source_dir, 'stops.txt')
    stops_hash = _calc_hash(stops_path)

    with open(os.path.join(cache_path, "stops.hash"), 'w+') as file:
        file.write(stops_hash)

    annotated_stops.to_csv(os.path.join(cache_path, 'annotated_stops.txt'), index=False)


def _calc_hash(path):
    hash_sha256 = hashlib.sha256()
    with open(path, 'rb') as file:
        for block in iter(lambda: file.read(hash_sha256.block_size), b""):
            hash_sha256.update(block)
    return hash_sha256.hexdigest()


def md5(fname):
    hash_md5 = hashlib.md5()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(hash_md5.block_size()), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def _annotate_stops(source_dir):
    annotated_stops = _load_location_from_cache(source_dir)
    if not annotated_stops.empty:
        return annotated_stops
    stops = pd.read_csv(os.path.join(source_dir, "stops.txt"))
    geolocator = Nominatim(user_agent="train2x_sensor_sim")
    geodecode = RateLimiter(geolocator.reverse, min_delay_seconds=1)
    print("Annotating each stop with its country:", flush=True)
    stops['location'] = stops[['stop_lat', 'stop_lon']].progress_apply(partial(geodecode, language='en'), axis=1)
    stops['country'] = stops['location'].apply(_location_to_country)
    _store_location_to_cache(source_dir, stops[['stop_id', 'stop_name', 'stop_lat', 'stop_lon', 'country']])
    return stops[['stop_id', 'stop_name', 'stop_lat', 'stop_lon', 'country']]


def _repair_stop_sequence(stop_times_group):
    stop_times_group.sort_values('stop_sequence', ascending=True, inplace=True)
    stop_times_group['stop_sequence'] = stop_times_group.reset_index().index
    return stop_times_group


def filter_country(stops, stop_times, trips, source_dir, country_regex):
    annotated_stops = _annotate_stops(source_dir)
    included_stops = annotated_stops[annotated_stops.country.str.match(country_regex, na=False)][[
        'stop_id', 'stop_name', 'stop_lat', 'stop_lon']]
    filtered_stops = pd.merge(included_stops, stops, how="inner", on=['stop_id'])[[
        'stop_id', 'stop_name_x', 'stop_lat_x', 'stop_lon_x']].rename(columns={
        'stop_name_x': 'stop_name', 'stop_lat_x': 'stop_lat', 'stop_lon_x': 'stop_lon'})
    filtered_stop_times = pd.merge(filtered_stops, stop_times, how="inner", on="stop_id")[['trip_id', 'arrival_time', 'departure_time', 'stop_sequence', 'stop_id']]
    filtered_stop_times = filtered_stop_times.groupby('trip_id', group_keys=False, as_index=False).apply(
        _repair_stop_sequence
    )[['trip_id', 'arrival_time', 'departure_time', 'stop_sequence', 'stop_id']]

    headsignes = filtered_stop_times.drop_duplicates(subset="trip_id", keep="last").reset_index()
    tripId_to_headsign = pd.merge(headsignes, annotated_stops, how="inner", on="stop_id")
    intermediate_trips = pd.merge(tripId_to_headsign, trips, how="inner", on="trip_id")[[
        'route_id', 'service_id', 'trip_id', 'stop_name']]
    trips = intermediate_trips.rename(columns={'stop_name': 'trip_headsign'}).sort_values('trip_id')

    return filtered_stops, filtered_stop_times, trips
