import os

from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


class CustomModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        self.model = deserializer.deserialize(filename)
        predictor_name = os.environ.get("MODEL_WRAPPER_PREDICT_METHOD", "predict")
        print("Using {} as predictor method".format(predictor_name))
        self.predictor_function = getattr(self.model, predictor_name)

    #TODO: this should check if predict batch is implemented before
    # if it is send to predict batch else predict_one
    # user would implement either predict or predict batch only
    def predict(self, *args, **kwargs):
        return self.predictor_function(*args, **kwargs)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'custom'

    def example(self):
        if hasattr(self.model, 'example'):
            return self.model.example()
        return ""

    def describe(self):
        if hasattr(self.model, 'describe'):
            return self.model.describe()
        return ""

    def version(self):
        if hasattr(self.model, 'version'):
            return self.model.version()
        return ""
