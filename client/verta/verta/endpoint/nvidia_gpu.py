from enum import Enum


class NvidiaGPU:
    NUM_GPUS_ERR_MSG = "`number` must be a number greater than 0"
    MODEL_ERR_MSG = "`model` must be an instance of `verta.endpoint.NvidiaGPUModel`"

    def __init__(self, number, model=None):
        self._validate_number_of_gpus(number)
        if model is not None:
            # convert to NvidiaGPUModel instance if passed in as str
            model = self._validate_model(model)
        self.number = number
        self.model = model

    def _validate_number_of_gpus(self, number_of_gpus):
        if not isinstance(number_of_gpus, int):
            raise TypeError(self.NUM_GPUS_ERR_MSG)
        if number_of_gpus <= 0:
            raise ValueError(self.NUM_GPUS_ERR_MSG)

    def _validate_model(self, model):
        if isinstance(model, str):
            model = NvidiaGPUModel(model)
        if not isinstance(model, NvidiaGPUModel):
            raise TypeError(self.MODEL_ERR_MSG)
        return model

    def _as_dict(self):
        d = dict()
        d["number"] = self.number
        if self.model is not None:
            d["model"] = self.model.value

        return d

    @classmethod
    def _from_dict(cls, rule_dict):
        return cls(**rule_dict)


class NvidiaGPUModel(str, Enum):
    T4 = "T4"
    V100 = "V100"
