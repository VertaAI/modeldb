from verta.deserialization.wrappers.model.abc_model_wrapper import ABCModelWrapper
from verta.deserialization.wrappers.model.pytorch_model_wrapper import PytorchModelWrapper
from verta.deserialization.wrappers.model.scikit_model_wrapper import ScikitModelWrapper
from verta.deserialization.wrappers.model.tensorflow_model_wrapper import TensorflowModelWrapper
from verta.deserialization.wrappers.model.xgboost_model_wrapper import XgboostModelWrapper
from verta.deserialization.wrappers.model.custom_model import CustomModelWrapper
from verta.deserialization.wrappers.model.callable_model import CallableModelWrapper
from verta.deserialization.wrappers.model.class_model import ClassModelWrapper
from verta.deserialization.wrappers.wrapper_config import WrapperConfig
from verta.deserialization.deserializer_factory import make_deserializer


def make_model_wrapper(config  # type: WrapperConfig
                       ):
    #  type: (...) -> ABCModelWrapper
    model_wrappers = (
        ScikitModelWrapper,
        XgboostModelWrapper,
        PytorchModelWrapper,
        TensorflowModelWrapper,
        CustomModelWrapper,
        CallableModelWrapper,
        ClassModelWrapper,
    )

    model_classes = {model_wrapper.model_type(): model_wrapper for model_wrapper in model_wrappers}

    return model_classes[config.MODEL_TYPE](config.MODEL_FILENAME, make_deserializer(config.DESERIALIZATION))
