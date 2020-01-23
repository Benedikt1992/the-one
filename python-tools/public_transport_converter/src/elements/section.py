class Section:
    def __init__(self, a, b):
        if a < b:
            self.section1 = a
            self.section2 = b
        else:
            self.section1 = b
            self.section2 = a

    def get_section(self):
        return self.section1, self.section2

    def __getitem__(self, item):
        if item == 0:
            return self.section1
        if item == 1:
            return self.section2
        raise IndexError()

    def __len__(self):
        return 2

    def __hash__(self):
        return hash((self.section1, self.section2))

    def __eq__(self, other):
        if isinstance(other, Section):
            return (self.section1 == other.section1 and self.section2 == other.section2)
        raise NotImplementedError("Comparison is not implemented for type {}".format(type(other)))

    def __ne__(self, other):
        return not self.__eq__(other)
