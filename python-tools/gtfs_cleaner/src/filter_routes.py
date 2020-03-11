import pandas as pd


def filter_routes(routes: pd.DataFrame, trips: pd.DataFrame, stop_times: pd.DataFrame, stops: pd.DataFrame, include_regex):
    included_routes = routes[routes.route_long_name.str.match(include_regex)]
    filtered_trips = pd.merge(included_routes, trips, how="left", on="route_id")[[
        'route_id', 'service_id', 'trip_id', 'trip_headsign']]
    filtered_stop_times = pd.merge(filtered_trips, stop_times, how="left", on="trip_id")[[
        'trip_id', 'arrival_time', 'departure_time', 'stop_id', 'stop_sequence']]
    filtered_stops = pd.merge(filtered_stop_times, stops, how="left", on="stop_id").drop_duplicates(subset="stop_id", keep="first")[[
        'stop_id', 'stop_name', 'stop_lat', 'stop_lon']]

    return included_routes, filtered_trips, filtered_stop_times, filtered_stops
