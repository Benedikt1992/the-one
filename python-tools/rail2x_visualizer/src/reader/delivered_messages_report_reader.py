import csv


class DeliveredMessagesReportReader:
    time = 0
    id = 1
    size = 2
    hopcount = 3
    delivery_time = 4
    from_host = 5
    to_host = 6
    remaining_ttl = 7
    isResponse = 8
    path = 9

    def __init__(self, file_path):
        with open(file_path, 'r') as file:
            csv_reader = csv.reader(file, delimiter=' ')
            header = next(csv_reader)

            # The report header starts with '#' and has 2 spaces as delimiter. Therefor, adapt the indices.
            if header[self.time + 1] != "time" or \
                    header[self.id + 2] != "ID" or \
                    header[self.size + 3] != "size" or \
                    header[self.hopcount + 4] != "hopcount" or \
                    header[self.delivery_time + 5] != "deliveryTime" or \
                    header[self.from_host + 6] != "fromHost" or \
                    header[self.to_host + 7] != "toHost" or \
                    header[self.remaining_ttl + 8] != "remainingTtl" or \
                    header[self.isResponse + 9] != "isResponse" or \
                    header[self.path + 10] != "path":
                raise ValueError("The provided DeliveredMessagesReport has the wrong format.")

            self.rows = []
            for row in csv_reader:
                self.rows.append(row)

    def get_deliveries(self, destination):
        deliveries = []
        for row in self.rows:
            if row[self.to_host] == destination:
                deliveries.append((float(row[self.delivery_time]), row[self.id]))
        return deliveries

    def get_hops(self, destination=None):
        hops = []
        for row in self.rows:
            if destination is None or row[self.to_host] == destination:
                hops.append(int(row[self.hopcount]))
        return hops
