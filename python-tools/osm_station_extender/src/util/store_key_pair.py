import argparse


class StoreKeyPair(argparse.Action):
    def __call__(self, parser, namespace, values, option_string=None):
        my_list = []
        for kv in values.split(","):
            k, v = kv.split("=")
            my_list.append((k, v))
        setattr(namespace, self.dest, my_list)
