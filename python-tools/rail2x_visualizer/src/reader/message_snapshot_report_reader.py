import csv
import re


class MessageSnapshotReportReader:
    time = 0
    nodeId = 1
    messages = 2
    buffer = 3

    def __init__(self, file_path):
        with open(file_path, 'r') as file:
            csv_reader = csv.reader(file, delimiter=',')
            header = next(csv_reader)

            # The report header starts with '#' and has 2 spaces as delimiter. Therefor, adapt the indices.
            if header[self.time] != "time" or \
                    header[self.nodeId] != "nodeId" or \
                    header[self.messages] != "messages" or \
                    header[self.buffer] != "bufferOccupancy":
                raise ValueError("The provided MessageSnapshotReport has the wrong format.")

            current_time = 0
            last_time = 0
            self.snapshots = []
            snapshot = []
            for row in csv_reader:
                pattern = r'\[(?P<time>[0-9]+)\]'
                match = re.search(pattern, row[self.time])
                if match:
                    last_time = current_time
                    current_time = int(match.group('time'))
                    self.snapshots.append(snapshot)
                    snapshot = []
                    continue
                snapshot.append(row)

            self.interval = current_time - last_time

    def get_host_groups(self):
        groups = set()
        for row in self.snapshots[1]:
            regex = r'(?P<group>[a-zA-Z]+)[0-9]+'
            matches = re.search(regex, row[self.nodeId])
            groups.add(matches.group('group'))
        return groups

    def get_lines(self, group=r'.*'):
        lines = {}
        intervals = []
        for index, snapshot in enumerate(self.snapshots):
            for row in snapshot:
                if re.search(group, row[self.nodeId]):
                    try:
                        lines[row[self.nodeId]].append(int(row[self.messages]))
                    except KeyError:
                        lines[row[self.nodeId]] = [int(row[self.messages])]
            if snapshot:
                intervals.append(index * self.interval)

        return intervals, lines.values()
