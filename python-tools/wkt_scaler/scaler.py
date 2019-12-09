max_x = 0.0
max_y = 0.0

with open('ice-karlsruhe.osm.wkt', 'r') as source_file:
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

scaling_factor_x = int(max_x / 4500) + 1
scaling_factor_y = int(max_y / 3400) + 1
scaling_factor = max(scaling_factor_x, scaling_factor_y)

with open('ice-karlsruhe.osm.wkt', 'r') as source_file, open('scaled' + str(scaling_factor) + '-ice-karlsruhe.osm.wkt', 'w') as dst_file:
    for line in source_file:
        coordinate_list = line[len('LINESTRING ('):-2]
        coordinates = coordinate_list.split(',')
        scaled_coordinates = []
        for coordinate in coordinates:
            coordinate = coordinate.strip()
            try:
                x, y = coordinate.split(' ')
                x = float(x) / scaling_factor
                y = float(y) / scaling_factor
                scaled_coordinates.append(str(x) + ' ' + str(y))
            except ValueError as e:
                print("Unexpected coordinates: '" + coordinate + "'")
                raise e
        dst_file.write('LINESTRING (' + ', '.join(scaled_coordinates) + ')\n')
