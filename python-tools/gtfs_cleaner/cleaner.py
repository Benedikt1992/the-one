import argparse
import shutil

from src.filter_routes import filter_routes
from src.filter_country import filter_country

import pandas as pd
import os

package_path = os.path.dirname(__file__)

SOURCE_DIR_DEFAULT = os.path.join(package_path, 'testdata', 'raw-data')
OUT_DIR_DEFAULT = os.path.join(package_path, 'testdata', 'data')
# List of nightlines: https://de.wikipedia.org/wiki/Liste_der_Nachtzugverbindungen_in_Deutschland#ICE
ROUTE_REGEX_DEFAULT = '^ICE(?![ ]?(208|209|949|948|1048|1049|990|781|618|619|1271|992|993|698|699)\s?$)'
COUNTRY_DEFAULT = 'Germany'


def _load_files(directory):
    stop_times = pd.read_csv(os.path.join(directory, "stop_times.txt"))
    trips = pd.read_csv(os.path.join(directory, "trips.txt"))
    routes = pd.read_csv(os.path.join(directory, "routes.txt"))
    stops = pd.read_csv(os.path.join(directory, "stops.txt"))

    return routes, trips, stop_times, stops


def _store_files(routes: pd.DataFrame, trips, stop_times, stops, source, destination):
    os.makedirs(destination, exist_ok=True)
    for f in os.listdir(destination):
        if f.endswith('.txt') or f.endswith('.sqlite'):
            os.remove(os.path.join(destination, f))

    for f in os.listdir(source):
        if f.endswith('.txt'):
            s = os.path.join(source, f)
            d = os.path.join(destination, f)
            shutil.copyfile(s, d)

    routes.to_csv(os.path.join(destination, 'routes.txt'), index=False)
    trips.to_csv(os.path.join(destination, 'trips.txt'), index=False)
    stop_times.to_csv(os.path.join(destination, 'stop_times.txt'), index=False)
    stops.to_csv(os.path.join(destination, 'stops.txt'), index=False)


def _load_arguments():
    parser = argparse.ArgumentParser(description="Clean GTFS data for the simulation process.",
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    parser.add_argument('-s', '--source', metavar='<source-path>', nargs='?', default=SOURCE_DIR_DEFAULT,
                        help='Source directory of the extracted GTFS data.')
    parser.add_argument('-d', '--destination', metavar='<destination-path>', nargs='?', default=OUT_DIR_DEFAULT,
                        help='Destination directory of the cleaned GTFS data.')
    parser.add_argument('-r', '--route-filter', metavar='<regex>', nargs='?', default=ROUTE_REGEX_DEFAULT,
                        help="Regular expression describing which routes should be kept for simulation.")
    parser.add_argument('-c', '--country-filter', metavar='<regex>', nargs='?', default=COUNTRY_DEFAULT,
                        help="Regular expression describing which countries should be kept for simulation.")

    args = parser.parse_args()

    if not os.path.isdir(args.source or ''):
        raise parser.error("Source is not a directory.")
    if not os.path.isdir(args.destination or ''):
        raise parser.error("Destination is not a directory.")

    return args


if __name__ == '__main__':
    args = _load_arguments()
    routes, trips, stop_times, stops = _load_files(args.source)
    if args.route_filter is not None and args.route_filter is not '':
        routes, trips, stop_times, stops = filter_routes(routes, trips, stop_times, stops, args.route_filter)
    if args.country_filter is not None and args.country_filter is not '':
        stops, stop_times, trips = filter_country(stops, stop_times, trips, args.source, args.country_filter)
    _store_files(routes, trips, stop_times, stops, args.source, args.destination)

