from verta.deserialization.abc_deserializer import ABCDeserializer


class CloudpickleDeserializer(ABCDeserializer):
    def deserialize(self, filename):
        import cloudpickle
        with open(filename, 'rb') as f:
            return cloudpickle.load(f)

    @staticmethod
    def deserializer_type():
        return 'cloudpickle'
