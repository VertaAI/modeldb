from verta.deserialization.abc_deserializer import ABCDeserializer


class PickleDeserializer(ABCDeserializer):
    def deserialize(self, filename):
        import pickle
        with open(filename, 'rb') as f:
            return pickle.load(f)

    @staticmethod
    def deserializer_type():
        return 'pickle'
