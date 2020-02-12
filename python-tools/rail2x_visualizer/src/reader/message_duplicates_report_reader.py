import csv


class MessageDuplicatesReportReader:
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
        hosts = {}

        for row in self.rows:
            if prefix in row[self.host]:
                duplicates = hosts.get(row[self.host], [])
                duplicates.append((row[self.message], int(row[self.duplicates])))
                hosts[row[self.host]] = duplicates

        return hosts
