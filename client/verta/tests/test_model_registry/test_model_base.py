# -*- coding: utf-8 -*-

import pytest

from verta.registry import VertaModelBase


def test_abstract_methods():
    """Abstract methods must be implemented."""
    ERROR_MSG_REGEX = "^Can't instantiate abstract class .* with abstract method"

    class Model1(VertaModelBase):
        pass

    class Model2(VertaModelBase):
        def __init__(self, artifacts):
            pass

    class Model3(VertaModelBase):
        def predict(self, input):
            pass

    for model_cls in [Model1, Model2, Model3]:
        with pytest.raises(TypeError, match=ERROR_MSG_REGEX):
            model_cls(None)
