from setuptools import setup, find_packages
setup(
    name="Public Transport Converter",
    version="0.1",
    packages=find_packages(),
    scripts=['public_transport_converter.py'],

    description="This package takes osm rail data and gtfs schedule and generates a map and schedules for the ONE simulator",

    # Project uses reStructuredText, so ensure that the docutils get
    # installed or upgraded on the target machine
    install_requires=['pygtfs',
                      'geopy',
                      'networkx',
                      'simplejson>=2.1'
                      ],
)
