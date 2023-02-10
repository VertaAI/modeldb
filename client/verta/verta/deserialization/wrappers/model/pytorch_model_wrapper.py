from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper


class PytorchModelWrapper(ABCModelWrapper):
    def __init__(self,
                 filename,  # type: str
                 deserializer,  # type: ABCDeserializer
                 ):
        self.model = deserializer.deserialize(filename)

    def predict(self, *args, **kwargs):
        import torch
        with torch.no_grad():
            return self.model(*args, **kwargs)

    def process_input(self, inp):
        import torch
        return torch.tensor(inp)

    @staticmethod
    def model_type():
        # type: (...) -> str
        return 'torch'
