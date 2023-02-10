from verta.deserialization.abc_deserializer import ABCDeserializer


class KerasDeserializer(ABCDeserializer):
    def __init__(self):
        self.session = None
        self.graph = None

    def deserialize(self, filename):
        import tensorflow
        self.graph = tensorflow.Graph()
        with self.graph.as_default():
            self.session = tensorflow.Session()
            with self.session.as_default():
                return tensorflow.keras.models.load_model(filename)

    @staticmethod
    def deserializer_type():
        return 'keras'
