from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


class CallableModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        self.model = deserializer.deserialize(filename)

    #TODO: this should check if predict batch is implemented before
    # if it is send to predict batch else predict_one
    # user would implement either predict or predict batch only
    def predict(self, *args, **kwargs):
        return self.model(*args, **kwargs)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'callable'
