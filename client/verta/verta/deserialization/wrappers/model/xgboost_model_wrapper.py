from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


class XgboostModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        self.model = deserializer.deserialize(filename)

    def predict(self, *args, **kwargs):
        return self.model.predict(*args, **kwargs)

    def process_input(self, inp):
        import numpy
        return numpy.array(inp)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'xgboost'
