import pytest

from .. import utils

import verta.dataset
import verta.environment


class TestModelVersion:
    def test_get_by_name(self, client, model_version):
        retrieved_model_version = client.set_model_version(model_version.name)
        assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, client, model_version):
        retrieved_model_version = client.get_or_create_repository(id=model_version.id)
        assert model_version.id == retrieved_model_version.id