from setuptools import setup, find_packages
setup(
    name="Visualizer for results produced by the rail2x configuration.",
    version="0.1",
    packages=find_packages(),
    scripts=['visualizer.py'],

    description="This package creates graphs to visualize the results of the ONE simulation of rail2x data.",

    # Project uses reStructuredText, so ensure that the docutils get
    # installed or upgraded on the target machine
    install_requires=['matplotlib'],
)
