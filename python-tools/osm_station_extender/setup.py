from setuptools import setup, find_packages
setup(
    name="OSM Station Extender",
    version="0.1",
    packages=find_packages(),
    scripts=['osm_station_extender.py'],

    description="This package takes osm rail data and gtfs stops and combines them in one map.",

    # Project uses reStructuredText, so ensure that the docutils get
    # installed or upgraded on the target machine
    install_requires=[],
)
