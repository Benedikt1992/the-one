import csv
import re


class MessageProcessingReportReader:
    host = 0
    outgoing = 1
    incoming = 2

    def __init__(self, file_path):
        with open(file_path, 'r') as file:
            csv_reader = csv.reader(file, delimiter=',')
            header = next(csv_reader)

            # The report header starts with '#' and has 2 spaces as delimiter. Therefor, adapt the indices.
            if header[self.host] != "host" or \
                    header[self.outgoing] != "outgoing" or \
                    header[self.incoming] != "incoming":
                raise ValueError("The provided DeliveredMessagesReport has the wrong format.")

            self.rows = []
            for row in csv_reader:
                self.rows.append(row)

    def get_host_groups(self):
        groups = set()
        for row in self.rows:
            regex = r'(?P<group>[a-zA-Z]+)[0-9]+'
            matches = re.search(regex, row[self.host])
            groups.add(matches.group('group'))
        return groups

    def get_incoming_distribution(self, host_group=r'.*'):
        distribution = []
        for row in self.rows:
            if re.search(host_group, row[self.host]):
                distribution.append(int(row[self.incoming]))
        return distribution

    def get_outgoing_distribution(self, host_group=r'.*'):
        distribution = []
        for row in self.rows:
            if re.search(host_group, row[self.host]):
                distribution.append(int(row[self.outgoing]))
        return distribution

