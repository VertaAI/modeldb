# -*- coding: utf-8 -*-

import os
import pickle
import tempfile

import pytest

from ..models.standard_models import verta_models, incomplete_verta_models


def test_abstract_methods():
    """Abstract methods must be implemented."""
    ERROR_MSG_REGEX = "^Can't instantiate abstract class .* with abstract method"

    # create artifact file for model init
    with tempfile.NamedTemporaryFile(delete=False) as tempf:
        pickle.dump("foo", tempf)

    try:
        for model_cls in verta_models():
            assert model_cls({model_cls.ARTIFACT_KEY: tempf.name})

        for model_cls in incomplete_verta_models():
            with pytest.raises(TypeError, match=ERROR_MSG_REGEX):
                model_cls({})
    finally:
        os.remove(tempf.name)
