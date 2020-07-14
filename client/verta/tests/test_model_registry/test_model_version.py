import pytest

from .. import utils

import verta.dataset
import verta.environment


class TestModelVersion:
    def test_get_by_name(self, client, model_version):
        retrieved_model_version = client.set_model_version(model_version.name)
        assert retrieved_model_version.id == model_version.id