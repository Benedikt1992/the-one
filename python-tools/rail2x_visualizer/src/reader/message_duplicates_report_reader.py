import csv


class MessageDuplicatesReportReader:
    """
    This class processes a MessageDuplicatesReport from the ONE simulator.
    """
    host = 0
    message = 1
    duplicates = 2

    def __init__(self, file_path):
        with open(file_path, 'r') as file:
            csv_reader = csv.reader(file, delimiter=',')
            header = next(csv_reader)

            # The report header starts with '#' and has 2 spaces as delimiter. Therefor, adapt the indices.
            if header[self.host] != "host" or \
                    header[self.message] != "message" or \
                    header[self.duplicates] != "duplicates":
                raise ValueError("The provided MessageDuplicatesReport has the wrong format.")

            self.rows = []
            for row in csv_reader:
                self.rows.append(row)

    def get_duplicates_grouped_by_host(self, prefix=""):
        """
        Get the amount of deliveries for a single message to a host.
        Optionally filter the desired host with a prefix.
        :param prefix: A string that all desired hosts share
        :return: dict of lists of tupley (message name, amount of deliveries)
        """
        hosts = {}

        for row in self.rows:
            if prefix in row[self.host]:
                duplicates = hosts.get(row[self.host], [])
                duplicates.append((row[self.message], int(row[self.duplicates])))
                hosts[row[self.host]] = duplicates

        return hosts
