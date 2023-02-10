import json
import os

from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


ARTIFACTS_JSON_FILEPATH = os.environ.get("ARTIFACTS_JSON_FILEPATH", "/app/artifacts.json")


class ClassModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        if os.path.isfile(ARTIFACTS_JSON_FILEPATH):
            with open(ARTIFACTS_JSON_FILEPATH) as f:
                artifacts = json.load(f)
        else:
            artifacts = {}

        Model = deserializer.deserialize(filename)
        self.model = Model(
            artifacts=artifacts,
        )
        predictor_name = os.environ.get("MODEL_WRAPPER_PREDICT_METHOD", "predict")
        print("Using {} as predictor method".format(predictor_name))
        self.predictor_function = getattr(self.model, predictor_name)

    def predict(self, *args, **kwargs):
        return self.predictor_function(*args, **kwargs)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'class'

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
