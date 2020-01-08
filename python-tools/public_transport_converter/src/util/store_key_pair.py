import argparse


class StoreKeyPair(argparse.Action):
    """
    This ArgParse action stores arguments in the form of 'KEY1=VAL1,KEY2=VAL2,...' as
    list of tuples in the form '[(KEY1,VAL1), (KEY2,VAL2),...]'
    """
    def __call__(self, parser, namespace, values, option_string=None):
        my_list = []
        for kv in values.split(","):
            k, v = kv.split("=")
            my_list.append((k, v))
        setattr(namespace, self.dest, my_list)
