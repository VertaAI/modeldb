import json
import collections
from scipy import spatial
import numpy as np


class DiscreteHistogram(object):
    def __init__(self, values, labels=None):
        self.values = values
        self.labels = labels
        if labels is None:
            self.labels = list(range(len(values)))

    def to_json(self):
        return {
            "discreteHistogram": {
                "buckets": self.labels,
                "data": self.values,
            }
        }


    @staticmethod
    def from_json(dct):
        try:
            buckets = dct['discreteHistogram']['buckets']
            data = dct['discreteHistogram']['data']
            return DiscreteHistogram(data, buckets)
        except:
            return dct


    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: I'm assuming labels are consistent
        if not (collections.Counter(self.labels) == collections.Counter(other.labels)):
            return -1  # fix error return
        else:
            # TODO: I'm assuming order of labels is consistent
            # normalize
            self_normalized = self.normalize()
            other_normalized = other.normalize()
            return spatial.distance.cosine(self_normalized, other_normalized)

    def normalize(self):
        total = sum(self.values)
        return [x * 1.0 / total for x in self.values]


class FloatHistogram(object):
    def __init__(self, values, limits):
        self.values = values
        self.limits = limits

    def to_json(self):
        return {
            "floatHistogram": {
                "bucketLimits": self.limits,
                "data": self.values,
            }
        }

    @staticmethod
    def from_json(dct):
        try:
            buckets = dct['floatHistogram']['bucketLimits']
            data = dct['floatHistogram']['data']
            return DiscreteHistogram(buckets, data)
        except:
            return dct


    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: I'm assuming labels are consistent
        if not (collections.Counter(self.limits) == collections.Counter(other.limits)):
            return -1  # fix error return
        else:
            # TODO: I'm assuming order of labels is consistent
            # normalize
            self_normalized = self.normalize()
            other_normalized = other.normalize()
            return spatial.distance.cosine(self_normalized, other_normalized)

    def normalize(self):
        total = sum(self.values)
        return [x * 1.0 / total for x in self.values]

class NumericValue(object):
    def __init__(self, value, label=None):
        self.value = value
        self.label = label

    def to_json(self):
        return {
            "numeric": {
                "value": self.value,
                "unit": self.label if self.label else ""
            }
        }

    @staticmethod
    def from_json(dct):
        if 'numeric' in dct:
            return NumericValue(dct["numeric"]["value"], dct["numeric"]["unit"] or None)
        else:
            return dct

    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: this value may be +ve or -ve. We need to decide correct behavior
        return (self.value - other.value)/self.value

class StringValue(object):
    def __init__(self, value, label=None):
        self.value = value
        self.label = label

    def to_json(self):
        return {
            "stringValue": {
                "value": self.value,
                "unit": self.label if self.label else ""
            }
        }

    @staticmethod
    def from_json(dct):
        if 'stringValue' in dct:
            return StringValue(dct["stringValue"]["value"], dct["stringValue"]["unit"] or None)
        else:
            return dct

    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        return 0 if self.value == other.value else 1

class Matrix(object):
    def __init__(self, values):
        self.values = values

    def to_json(self):
        return {
            "matrixValue": {
                "values": self.values
            }
        }

    @staticmethod
    def from_json(dct):
        if 'matrixValue' in dct:
            return Matrix(dct["matrixValue"]["value"], dct["matrixValue"]["unit"] or None)
        else:
            return dct

    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: add some distance function
        return np.matrix(self.values) - other.matrix(self.values)

class ConfusionMatrix(Matrix):
    def __init__(self, values, classes):
        self.values = values
        self.classes = classes

    def to_json(self):
        return {
            "confusionMatrixValue": {
                "values": self.values,
                "classes": self.classes
            }
        }

    @staticmethod
    def from_json(dct):
        if 'matrixValue' in dct:
            return Matrix(dct["matrixValue"]["values"], dct["matrixValue"]["classes"])
        else:
            return dct

    # TODO: diff function not useful here
    def diff(self, other):
        return -1

class Table(object):
    def __init__(self, values):
        self.values = values

    def to_json(self):
        return {
            "tableValue": {
                "values": self.values
            }
        }

    @staticmethod
    def from_json(dct):
        if 'tableValue' in dct:
            return Table(dct["tableValue"]["values"])
        else:
            return dct

    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: add some distance function
        return -1

# TODO: make it related to log_image
class Image(object):
    def __init__(self, values):
        self.values = values

    def to_json(self):
        return {
            "tableValue": {
                "values": self.values
            }
        }

    @staticmethod
    def from_json(dct):
        if 'tableValue' in dct:
            return Table(dct["tableValue"]["values"])
        else:
            return dct

    def __repr__(self):
        return "{}: {}".format(self.__class__.__name__, json.dumps(self.to_json()))

    def diff(self, other):
        # TODO: add some distance function
        return -1