from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


class TensorflowModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        import tensorflow
        if int(tensorflow.__version__.split('.')[0]) >= 2:
            import tensorflow.compat.v1 as tensorflow

        self.graph = tensorflow.Graph()
        with self.graph.as_default():
            self.session = tensorflow.Session()
            with self.session.as_default():
                self.model = tensorflow.keras.models.load_model(filename)

    def predict(self, *args, **kwargs):
        import tensorflow
        if int(tensorflow.__version__.split('.')[0]) >= 2:
            import tensorflow.compat.v1 as tensorflow

        tensorflow.keras.backend.set_session(self.session)
        with self.graph.as_default():
            return self.model.predict(*args, **kwargs)

    def process_input(self, inp):
        import numpy
        return numpy.array(inp)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'tensorflow'
