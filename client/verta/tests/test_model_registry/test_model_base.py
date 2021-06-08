# -*- coding: utf-8 -*-

import pytest

from verta.registry import VertaModelBase


def test_abstract_methods():
    """Abstract methods must be implemented."""
    ERROR_MSG_REGEX = "^Can't instantiate abstract class .* with abstract method"

    class NoImpl(VertaModelBase):
        pass

    class OnlyInit(VertaModelBase):
        def __init__(self, artifacts):
            pass

    class OnlyPredict(VertaModelBase):
        def predict(self, input):
            pass

    class Both(VertaModelBase):
        def __init__(self, artifacts):
            pass

        def predict(self, input):
            pass

    for model_cls in [NoImpl, OnlyInit, OnlyPredict]:
        with pytest.raises(TypeError, match=ERROR_MSG_REGEX):
            model_cls(None)

    assert Both(None)
