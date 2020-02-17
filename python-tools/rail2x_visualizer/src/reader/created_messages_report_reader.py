import csv


class CreatedMessagesReportReader:
    """
    This class processes a CreatedMessagesReport from the ONE simulator
    """
    time = 0
    id = 1
    size = 2
    from_host = 3
    to_host = 4
    ttl = 5
    isResponse = 6

    def __init__(self, file_path):
        with open(file_path, 'r') as file:
            csv_reader = csv.reader(file, delimiter=' ')
            header = next(csv_reader)

            # The report header starts with '#' and has 2 spaces as delimiter. Therefor, adapt the indices.
            if header[self.time + 1] != "time" or \
                    header[self.id + 2] != "ID" or \
                    header[self.size + 3] != "size" or \
                    header[self.from_host + 4] != "fromHost" or \
                    header[self.to_host + 5] != "toHost" or \
                    header[self.ttl + 6] != "TTL" or \
                    header[self.isResponse + 7] != "isResponse":
                raise ValueError("The provided CreatedMessagesReport has the wrong format.")

            self.rows = []
            for row in csv_reader:
                self.rows.append(row)

    def get_messages_grouped_by_destination(self):
        """
        Find which messages were generated for which destination
        :return: {<destinantion name> : [Message Names]}
        """
        destinations = {}

        for row in self.rows:
            try:
                destinations[row[self.to_host]].append(row[self.id])
            except KeyError:
                destinations[row[self.to_host]] = [row[self.id]]

        return destinations

    def get_destinations(self):
        """
        Which nodes should receive any messages.
        :return: set of names of destination nodes.
        """
        destinations = set()

        for row in self.rows:
            destinations.add(row[self.to_host])

        return destinations

    def get_messages(self):
        """
        Which messages were generated
        :return: Set of all message names.
        """
        messages = set()

        for row in self.rows:
            messages.add(row[self.id])

        return messages