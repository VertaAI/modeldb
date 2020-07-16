import pytest
import verta

from .. import utils
import os

from sklearn.linear_model import LogisticRegression
import numpy as np


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

    def test_set_model(self, model_version):
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.set_model(classifier)

        # retrieve the classifier:
        retrieved_classfier = model_version.get_model()
        assert (retrieved_classfier.coef_ == original_coef).all()

        # overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.set_model(new_classifier, True)
        retrieved_classfier = model_version.get_model()
        assert (retrieved_classfier.coef_ == new_classifier.coef_).all()

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.set_model(new_classifier)

        assert "model already exists" in str(excinfo.value)

    def test_add_asset(self, model_version):
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.add_asset("coef", original_coef)

        # retrieve the asset:
        retrieved_coef = model_version.get_asset("coef")
        assert (retrieved_coef == original_coef).all()

        # Overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.add_asset("coef", new_classifier.coef_, True)
        retrieved_coef = model_version.get_asset("coef")
        assert (retrieved_coef == new_classifier.coef_).all()

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.add_asset("coef", new_classifier.coef_)

        assert "The key has been set" in str(excinfo.value)

    def test_add_asset_file(self, model_version):
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)
        os.remove(filename)

        model_version.add_asset("file", filename)

        # retrieve the asset:
        retrieved_file = model_version.get_asset("file")
        print(retrieved_file)

    def test_wrong_key(self, model_version):
        with pytest.raises(KeyError) as excinfo:
            model_version.get_model()

        assert "no model associated with this version" in str(excinfo.value)

        with pytest.raises(KeyError) as excinfo:
            model_version.get_asset("non-existing")

        assert "no artifact found with key non-existing" in str(excinfo.value)

