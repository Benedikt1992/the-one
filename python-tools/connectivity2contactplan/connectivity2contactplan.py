"""
This script transforms a report from the ConnectivityONEReport module into a
contact plan required by the ContactGraphRouter.
If  the simulation settings are provided the script will recognize one way connections
specified with a MessageTransferAcceptPolicy '.toReceivePolicy = -1'.
Important Note: The contact plan will not support to find path to these nodes!
This can be useful to define a host group that works as data generators only.
"""
import csv
import os
from argparse import ArgumentParser, ArgumentError


def load_settings(settings):
    if not settings:
        return None
    with open(settings, 'r') as file:
        settings_map = {}
        csv_reader = csv.reader(file, delimiter='=')
        for row in csv_reader:
            if "#" in row[0]:
                continue
            settings_map[row[0].strip()] = row[1].strip()
    return settings_map


def load_send_only_hosts(settings):
    send_only_hosts = set()
    if settings:
        host_groups = int(settings['Scenario.nrofHostGroups'])
        start_id = 0
        for i in range(1, host_groups + 1):
            mta_policy = "Group" + str(i) + ".mtaPolicy"
            amount = "Group" + str(i) + ".nrofHosts"
            if settings.get(mta_policy, None):
                policy_name = settings[mta_policy]
                send_only = policy_name + ".toReceivePolicy"
                policy = settings.get(send_only, None)
                if policy == "-1":
                    end_id = start_id + int(settings[amount])
                    for j in range(start_id, end_id):
                        send_only_hosts.add(j)
            start_id += int(settings[amount])
    return send_only_hosts


def extract_contact_points(report, send_only_hosts, end_time):
    contacts = {}
    open_connections = {}
    with open(report, 'r') as file:
        csv_reader = csv.reader(file, delimiter=' ')
        for row in csv_reader:
            time = row[0]
            partner1 = int(row[2])
            partner2 = int(row[3])
            change = row[4]
            if change == 'up':
                if partner1 not in send_only_hosts:
                    open_connection = open_connections.get(partner2, {})
                    open_connection[partner1] = Contact(partner1, time)
                    open_connections[partner2] = open_connection
                if partner2 not in send_only_hosts:
                    open_connection = open_connections.get(partner1, {})
                    open_connection[partner2] = Contact(partner2, time)
                    open_connections[partner1] = open_connection
            elif change == 'down':
                contact = open_connections.get(partner1, {}).pop(partner2, None)
                if contact:
                    contact.set_end(time)
                    contact_list = contacts.get(partner1, [])
                    contact_list.append(contact)
                    contacts[partner1] = contact_list

                contact = open_connections.get(partner2, {}).pop(partner1, None)
                if contact:
                    contact.set_end(time)
                    contact_list = contacts.get(partner2, [])
                    contact_list.append(contact)
                    contacts[partner2] = contact_list
    for host in open_connections.keys():
        for connection in open_connections[host].values():
            connection.set_end(end_time)
            contact_list = contacts.get(host, [])
            contact_list.append(contact)
            contacts[host] = contact_list
    return contacts


class Contact:
    def __init__(self, contact_partner, start):
        self.partner = contact_partner
        self.start = start
        self.end = None

    def set_end(self, end):
        if not self.end:
            self.end = end
        else:
            raise RuntimeError("Tries to set contact end time twice.")

    def __str__(self):
        return "{} {} {}".format(self.partner, self.start, self.end)


def connectivity2contactplan():
    parser = ArgumentParser(description='Transform ConnectivityONEReport to a contact plan')
    parser.add_argument('-r', '--report', required=True, help="The ConnectivityONEReport.")
    parser.add_argument('-s', '--settings', nargs='?', default='',
                        help="Settings used to generate the ConnectivityONEReport.")
    args = parser.parse_args()

    if not os.path.isfile(args.report):
        raise ArgumentError("The report option should point to the report file.")

    if args.settings and not os.path.isfile(args.settings):
        raise ArgumentError("The settings option should point to the settings file.")

    settings = load_settings(args.settings)
    send_only_hosts = load_send_only_hosts(settings)

    if settings:
        contacts = extract_contact_points(args.report, send_only_hosts, settings["Scenario.endTime"])
    else:
        contacts = extract_contact_points(args.report, send_only_hosts, "-1")

    output_dir = os.path.dirname(args.report)
    with open(os.path.join(output_dir, "contact_plan.txt"), 'w') as file:
        for node in contacts.keys():
            contact_strings = [str(x) for x in contacts[node]]
            file.write("{} ({})\n".format(node, ", ".join(contact_strings)))


if __name__ == "__main__":
    connectivity2contactplan()
