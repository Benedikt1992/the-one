import os
from argparse import ArgumentParser, ArgumentTypeError
from decimal import *
getcontext()

parser = ArgumentParser(description='This scaler scales a wkt file with LINESTRING entries to fit into a given area. Use -h to get help.')
parser.add_argument('-f', '--file', required=True, help="File which should be scaled.")
parser.add_argument('-x', '--max_x', nargs="?", default='4500', type=int, help="Maximum size of x dimension.")
parser.add_argument('-y', '--max_y', nargs="?", default='3400', type=int, help="Maximum size of y dimension.")
args = parser.parse_args()

MAX_SCALED_X = args.max_x
MAX_SCALED_Y = args.max_y
FILE = args.file
if not os.path.isfile(FILE):
    raise ArgumentTypeError("The given file argument is not a file: '{}'".format(FILE))
file_name = os.path.basename(FILE)
dir_name = os.path.dirname(FILE)
max_x = 0.0
max_y = 0.0

with open(FILE, 'r') as source_file:
    for line in source_file:
        coordinate_list = line[len('LINESTRING ('):-2]
        coordinates = coordinate_list.split(',')
        for coordinate in coordinates:
            coordinate = coordinate.strip()
            try:
                x, y = coordinate.split(' ')
                x = float(x)
                y = float(y)
                if x > max_x:
                    max_x = x
                if y > max_y:
                    max_y = y
            except ValueError as e:
                print("Unexpected coordinates: '" + coordinate + "'")
                raise e

scaling_factor_x = int(max_x / MAX_SCALED_X) + 1
scaling_factor_y = int(max_y / MAX_SCALED_Y) + 1
scaling_factor = Decimal(max(scaling_factor_x, scaling_factor_y))

with open(FILE, 'r') as source_file, open(os.path.join(dir_name, 'scaled' + str(scaling_factor) + '-' + file_name), 'w') as dst_file:
    for line in source_file:
        coordinate_list = line[len('LINESTRING ('):-2]
        coordinates = coordinate_list.split(',')
        scaled_coordinates = []
        for coordinate in coordinates:
            coordinate = coordinate.strip()
            try:
                x, y = coordinate.split(' ')
                x = Decimal(x) / scaling_factor
                y = Decimal(y) / scaling_factor
                scaled_coordinates.append(str(x) + ' ' + str(y))
            except ValueError as e:
                print("Unexpected coordinates: '" + coordinate + "'")
                raise e
        dst_file.write('LINESTRING (' + ', '.join(scaled_coordinates) + ')\n')
