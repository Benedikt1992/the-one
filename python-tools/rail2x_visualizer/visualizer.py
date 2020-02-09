import os
from argparse import ArgumentParser, ArgumentError


class Visualizer:
    def __init__(self):
        # TODO enable to auto process the results of a batched simulation run
        parser = ArgumentParser(description='Extend osm xml data by gtfs stations and connect them to their stop points')
        parser.add_argument('-r', '--reports', required=True, help="Directory of the generated reports.")
        parser.add_argument('-s', '--scenario', required=True, help='Name of the scenario which should be visualized (without the report names).')

        self.args = parser.parse_args()

        if not os.path.isdir(self.args.reports):
            raise ArgumentError("-r/--reports needs to be a directory.")
        # todo test if all reports are available

    def run(self):
        # TODO implement the different graphs from BP
        pass


if __name__ == "__main__":
    visualizer = Visualizer()
    visualizer.run()
