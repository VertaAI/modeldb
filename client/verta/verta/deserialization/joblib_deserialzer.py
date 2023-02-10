from verta.deserialization.abc_deserializer import ABCDeserializer


class JoblibDeserializer(ABCDeserializer):
    def deserialize(self, filename):
        import joblib
        return joblib.load(filename)

    @staticmethod
    def deserializer_type():
        return 'joblib'
