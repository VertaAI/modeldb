import pytest

from .. import utils

import verta.dataset
import verta.environment


class TestModelVersion:
    # def test_get_by_name(self, client, registered_model):
    #     model_version = registered_model.get_or_create_version()
    #     retrieved_model_version = registered_model.set_model_version(model_version.name)
    #     assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, registered_model):
        model_version = registered_model.get_or_create_version()
        print(model_version.id)
        retrieved_model_version = registered_model.get_or_create_version(id=model_version.id)
        assert model_version.id == retrieved_model_version.id