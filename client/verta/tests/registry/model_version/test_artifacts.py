# -*- coding: utf-8 -*-

import pytest

from verta._internal_utils import _artifact_utils


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestArtifacts:
    def test_log_artifact(self, model_version):
        np = pytest.importorskip("numpy")
        pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        key = "coef"

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.log_artifact(key, original_coef)

        # retrieve the artifact:
        retrieved_coef = model_version.get_artifact(key)
        assert np.array_equal(retrieved_coef, original_coef)
        artifact_msg = model_version._get_artifact_msg(key)
        assert artifact_msg.key == key
        assert artifact_msg.filename_extension == "pkl"

        # Overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_artifact(key, new_classifier.coef_, overwrite=True)
        retrieved_coef = model_version.get_artifact(key)
        assert np.array_equal(retrieved_coef, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.log_artifact(key, new_classifier.coef_)

        assert "The key has been set" in str(excinfo.value)

    def test_add_artifact_file(self, model_version, in_tempdir, random_data):
        filename = "tiny1.bin"
        FILE_CONTENTS = random_data
        with open(filename, "wb") as f:
            f.write(FILE_CONTENTS)
        model_version.log_artifact("file", filename)

        # retrieve the artifact:
        retrieved_file = model_version.get_artifact("file")
        assert retrieved_file.getvalue() == FILE_CONTENTS

    def test_wrong_key(self, model_version):
        with pytest.raises(KeyError) as excinfo:
            model_version.get_model()

        assert "no model associated with this version" in str(excinfo.value)

        with pytest.raises(KeyError) as excinfo:
            model_version.get_artifact("non-existing")

        assert "no artifact found with key non-existing" in str(excinfo.value)

        np = pytest.importorskip("numpy")
        with pytest.raises(ValueError) as excinfo:
            model_version.log_artifact("model", np.random.random((36, 12)))

        assert (
            'the key "model" is reserved for model; consider using log_model() instead'
            in str(excinfo.value)
        )

        with pytest.raises(ValueError) as excinfo:
            model_version.del_artifact("model")

        assert (
            "model can't be deleted through del_artifact(); consider using del_model() instead"
            in str(excinfo.value)
        )

    def test_del_artifact(self, registered_model):
        np = pytest.importorskip("numpy")
        pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        model_version.log_artifact("coef", classifier.coef_)
        model_version.log_artifact("coef-2", classifier.coef_)
        model_version.log_artifact("coef-3", classifier.coef_)

        model_version.del_artifact("coef-2")
        assert len(model_version.get_artifact_keys()) == 2

        model_version.del_artifact("coef")
        assert len(model_version.get_artifact_keys()) == 1

        model_version.del_artifact("coef-3")
        assert len(model_version.get_artifact_keys()) == 0

    def test_del_model(self, registered_model):
        np = pytest.importorskip("numpy")
        pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        model_version = registered_model.get_version(id=model_version.id)
        assert model_version.has_model
        model_version.del_model()
        assert not model_version.has_model

        model_version = registered_model.get_version(id=model_version.id)
        assert not model_version.has_model

    @pytest.mark.skip(reason="not implemented (see TODO in log_artifact())")
    def test_blocklisted_key_error(self, model_version):
        value = "foo"

        for key in _artifact_utils.BLOCKLISTED_KEYS:
            with pytest.raises(ValueError, match="please use a different key$"):
                model_version.log_artifact(key, value)
