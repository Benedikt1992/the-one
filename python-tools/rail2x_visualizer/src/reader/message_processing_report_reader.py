import csv
import re


class MessageProcessingReportReader:
    """
    This class processes a MessageProcessingReport from the ONE simulator.
    """
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
        """
        Find which host groups were reported.
        :return: set of host groups
        """
        groups = set()
        for row in self.rows:
            regex = r'(?P<group>[a-zA-Z]+)[0-9]+'
            matches = re.search(regex, row[self.host])
            groups.add(matches.group('group'))
        return groups

    def get_incoming_distribution(self, host_group=r'.*'):
        """
        Get a list of processed incoming messages.
        By default all hosts are included but can be filtered
        :param host_group: regex matching all desired hosts.
        :return: list of amount of processed incoming messages
        """
        distribution = []
        for row in self.rows:
            if re.search(host_group, row[self.host]):
                distribution.append(int(row[self.incoming]))
        return distribution

    def get_outgoing_distribution(self, host_group=r'.*'):
        """
        Get a list of processed outgoing messages.
        By default all hosts are included but can be filtered
        :param host_group: regex matching all desired hosts.
        :return: list of amount of processed outgoing messages
        """
        distribution = []
        for row in self.rows:
            if re.search(host_group, row[self.host]):
                distribution.append(int(row[self.outgoing]))
        return distribution

