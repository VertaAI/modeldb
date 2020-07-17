import pytest

from .. import utils

import verta.dataset
import verta.environment

import numpy as np

from sklearn.linear_model import LogisticRegression


class TestModelVersion:
    def test_get_by_name(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        retrieved_model_version = registered_model.get_version(name=model_version.name)
        assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, registered_model):
        model_version = registered_model.get_or_create_version()
        retrieved_model_version = registered_model.get_version(id=model_version.id)
        assert model_version.id == retrieved_model_version.id

    def test_get_by_clent(self, client):
        registered_model = client.set_registered_model()
        model_version = registered_model.get_or_create_version(name="my version")

        retrieved_model_version_by_id = client.get_registered_model_version(id=model_version.id)
        retrieved_model_version_by_name = client.get_registered_model_version(name=model_version.name)

        assert retrieved_model_version_by_id.id == model_version.id
        assert retrieved_model_version_by_name.id == model_version.id

        if registered_model:
            utils.delete_registered_model(registered_model.id, client._conn)

    def test_log_model(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        log_reg_model = LogisticRegression()
        model_version.log_model(log_reg_model)

        # reload the model version:
        model_version = registered_model.get_or_create_version(name="my version")
        assert model_version._msg.model.key == "model"

        # overwrite should work:
        model_version = registered_model.get_version(id=model_version.id)
        model_version.log_model(log_reg_model, True)

        with pytest.raises(ValueError) as excinfo:
            model_version = registered_model.get_version(id=model_version.id)
            model_version.log_model(log_reg_model)

        assert "model already exists" in str(excinfo.value)


    def test_log_artifact(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        log_reg_model = LogisticRegression()
        model_version.log_artifact("some-asset", log_reg_model)

        # Overwrite should work:
        model_version = registered_model.get_version(id=model_version.id)
        model_version.log_artifact("some-asset", log_reg_model, True)

        with pytest.raises(ValueError) as excinfo:
            model_version = registered_model.get_version(id=model_version.id)
            model_version.log_artifact("some-asset", log_reg_model)

        assert "The key has been set" in str(excinfo.value)

    def test_del_artifact(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_artifact("coef", classifier.coef_)

        model_version = registered_model.get_version(id=model_version.id)
        model_version.del_artifact("coef")

        model_version = registered_model.get_version(id=model_version.id)
        assert len(model_version._msg.assets) == 0
