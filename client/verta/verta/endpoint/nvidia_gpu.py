from enum import Enum


class NvidiaGPU:
    """
    Nvidia GPU resources

    The JSON equivalent for this is:
    .. code-block:: json

        {
            "resources": {"nvidia_gpu": {"number": 1, "model": "V100"}}
        }

    Parameters
    ----------
    number: int > 0
        Number of GPUs requested to run this endpoint's model.
    model: :class:`NvidiaGPUModel`, optional
        Model of GPU requested to run this endpoint's model. Currently available models are
        specified in the `NvidiaGPUModel` enum.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.resources import Resources, NvidiaGPU, NvidiaGPUModel
        resources = Resources(nvidia_gpu=NvidiaGPU(1, NvidiaGPUModel.V100)
    """

    NUM_GPUS_ERR_MSG = "`number` must be a number greater than 0"
    MODEL_ERR_MSG = "`model` must be an instance of `verta.endpoint.NvidiaGPUModel`"

    def __init__(self, number, model=None):
        self._validate_number_of_gpus(number)
        if model is not None:
            self._validate_model(model)
            # convert to NvidiaGPUModel instance if passed in as str
            model = NvidiaGPUModel(model)
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
    def _from_dict(cls, gpu_dict):
        return cls(**gpu_dict)

    def _to_hardware_compatibility_dict(self):
        """
        An internal method to convert this object to a dictionary that will be used when creating a
        build.
        """
        if self.number == 0:
            return None
        if self.model is None:
            return {"nvidia_gpu": {"all": True}}
        else:
            return {"nvidia_gpu": {self.model.value: True}}


class NvidiaGPUModel(str, Enum):
    T4 = "T4"
    V100 = "V100"
