from setuptools import setup, find_packages
setup(
    name="Filter and reduce a gtfs dataset",
    version="0.1",
    packages=find_packages(),
    scripts=['cleaner.py'],

    description="This cleaner filter and reduces a gtfs dataset. "
                "It is able to filter for specific set of routes within the data "
                "and to limit stops to a specific country",

    # Project uses reStructuredText, so ensure that the docutils get
    # installed or upgraded on the target machine
    install_requires=['pandas', 'geopy', 'tqdm'],
)
