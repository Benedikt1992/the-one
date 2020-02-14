import csv
import os

from string import Template


class ONESettingsReader:
    def __init__(self, file_path, simulation_wd):
        self.current_run = None
        self.simulation_working_dir = simulation_wd

        with open(file_path, 'r') as file:
            self.settings_runs = []
            settings_set = {}
            csv_reader = csv.reader(file, delimiter='=')
            header = next(csv_reader)

            if "# Settings for run" not in header[0]:
                raise ValueError("The provided Settings have the wrong format.")

            for row in csv_reader:
                if "# Settings for run" in row[0]:
                    self.settings_runs.append(settings_set)
                    settings_set = {}
                    continue
                settings_set[row[0].strip()] = row[1].strip()
            self.settings_runs.append(settings_set)

    def get_simulation_duration(self):
        return int(self.settings_runs[self.current_run]['Scenario.endTime'])

    def next_run(self):
        if self.current_run is None:
            self.current_run = 0
        else:
            self.current_run += 1
            if self.current_run >= len(self.settings_runs):
                return None
        return self.current_run

    def get_stationary_nodes(self):
        current_settings = self.settings_runs[self.current_run]
        host_groups = int(current_settings['Scenario.nrofHostGroups'])

        nodes = {}
        start_id = 0
        for i in range(1, host_groups + 1):
            movement_model = "Group" + str(i) + ".movementModel"
            if current_settings[movement_model] == "StationaryListMovement":
                group_id = "Group" + str(i) + ".groupID"
                location_file = "Group" + str(i) + ".nodeLocationsFile"
                location_file_path = os.path.join(self.simulation_working_dir, current_settings[location_file])
                nodes[current_settings[group_id]] = {'start': start_id, 'location': location_file_path}
            amount = "Group" + str(i) + ".nrofHosts"
            start_id += int(current_settings[amount])

        return nodes

    def get_scenario(self):
        scenario_pattern = self.settings_runs[self.current_run]['Scenario.name']
        t = self.ScenarioTemplate(scenario_pattern)
        scenario = t.safe_substitute(self.settings_runs[self.current_run])
        return scenario

    class ScenarioTemplate(Template):
        pattern = r"""
        (?:
            %%(?P<braced>[a-zA-Z0-9.]*)%%   | # find braced with %%
            (?P<invalid>)                   | # ignore
            (?P<escaped>)                   | # ignore
            (?P<named>)                       # ignore
        )
        """

