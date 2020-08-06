import tarfile

import pytest
import requests

import verta

from .. import utils
import os

import verta.dataset
from verta.environment import Python


class TestMDBIntegration:
    def test_from_run(self, experiment_run, model_for_deployment, registered_model):
        np = pytest.importorskip("numpy")

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        artifact = np.random.random((36, 12))
        experiment_run.log_artifact("some-artifact", artifact)

        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )

        env_str = str(model_version.get_environment())
        assert 'scikit-learn' in env_str
        assert 'Python' in env_str

        assert model_for_deployment['model'].get_params() == model_version.get_model().get_params()
        assert np.array_equal(model_version.get_artifact("some-artifact"), artifact)


class TestModelVersion:

    def test_create(self, registered_model):
        name = verta._internal_utils._utils.generate_default_name()
        assert registered_model.create_version(name)
        with pytest.raises(requests.HTTPError) as excinfo:
            assert registered_model.create_version(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value

    def test_get_by_name(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        retrieved_model_version = registered_model.get_version(name=model_version.name)
        assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, registered_model):
        model_version = registered_model.get_or_create_version()
        retrieved_model_version = registered_model.get_version(id=model_version.id)
        assert model_version.id == retrieved_model_version.id

    def test_repr(self, model_version):
        model_version.add_labels(["tag1", "tag2"])
        repr = str(model_version)

        assert model_version.name in repr
        assert str(model_version.id) in repr
        assert str(model_version.registered_model_id) in repr
        assert str(model_version.get_labels()) in repr

        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        model_version.log_model(classifier)
        model_version.log_artifact("coef", classifier.coef_)
        repr = str(model_version)
        assert "model" in repr
        assert "coef" in repr

    def test_get_by_client(self, client):
        registered_model = client.set_registered_model()
        model_version = registered_model.get_or_create_version(name="my version")

        retrieved_model_version_by_id = client.get_registered_model_version(model_version.id)

        assert retrieved_model_version_by_id.id == model_version.id

    def test_log_model(self, model_version):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.log_model(classifier)

        # retrieve the classifier:
        retrieved_classfier = model_version.get_model()
        assert np.array_equal(retrieved_classfier.coef_, original_coef)

        # overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(new_classifier, overwrite=True)
        retrieved_classfier = model_version.get_model()
        assert np.array_equal(retrieved_classfier.coef_, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.log_model(new_classifier)

        assert "model already exists" in str(excinfo.value)

    def test_log_artifact(self, model_version):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.log_artifact("coef", original_coef)

        # retrieve the artifact:
        retrieved_coef = model_version.get_artifact("coef")
        assert np.array_equal(retrieved_coef, original_coef)

        # Overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_artifact("coef", new_classifier.coef_, True)
        retrieved_coef = model_version.get_artifact("coef")
        assert np.array_equal(retrieved_coef, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.log_artifact("coef", new_classifier.coef_)

        assert "The key has been set" in str(excinfo.value)

    def test_add_artifact_file(self, model_version, in_tempdir):
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
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

        assert "the key \"model\" is reserved for model; consider using log_model() instead" in str(excinfo.value)

        with pytest.raises(ValueError) as excinfo:
            model_version.del_artifact("model")

        assert "model can't be deleted through del_artifact(); consider using del_model() instead" in str(excinfo.value)

    def test_del_artifact(self, registered_model):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
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
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        model_version = registered_model.get_version(id=model_version.id)
        assert model_version.has_model
        model_version.del_model()
        assert (not model_version.has_model)

        model_version = registered_model.get_version(id=model_version.id)
        assert (not model_version.has_model)

    def test_log_environment(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        model_version.log_environment(env)

        model_version = registered_model.get_version(id=model_version.id)
        assert str(env) == str(model_version.get_environment())

    def test_del_environment(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        model_version.log_environment(env)
        model_version.del_environment()

        model_version = registered_model.get_version(id=model_version.id)
        assert not model_version.has_environment

        with pytest.raises(RuntimeError) as excinfo:
            model_version.get_environment()

        assert "environment was not previously set" in str(excinfo.value)

    def test_labels(self, client, created_registered_models):
        registered_model = client.set_registered_model()
        created_registered_models.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")

        model_version.add_label("tag1")
        model_version.add_label("tag2")
        model_version.add_label("tag3")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]
        model_version.del_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.del_label("tag4")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.add_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]

        model_version.add_labels(["tag2", "tag4", "tag1", "tag5"])
        assert model_version.get_labels() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

    def test_description(self, client, created_registered_models):
        desc = "description"
        registered_model = client.get_or_create_registered_model()
        created_registered_models.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")
        model_version.set_description(desc)
        assert desc == model_version.get_description()

    def test_find(self, client):
        name = "registered_model_test"
        registered_model = client.set_registered_model()
        model_version = registered_model.get_or_create_version(name=name)

        find_result = registered_model.versions.find(["version == '{}'".format(name)])
        assert len(find_result) == 1
        for item in find_result:
            assert item._msg == model_version._msg

        tag_name = name + "_tag"
        versions = {name + "1": registered_model.get_or_create_version(name + "1"),
                    name + "2": registered_model.get_or_create_version(name + "2")}
        versions[name + "1"].add_label(tag_name)
        versions[name + "2"].add_label(tag_name)
        versions[name + "2"].add_label("label2")

        for version in versions:
            versions[version] = registered_model.get_version(version)

        find_result = registered_model.versions.find(["labels == \"{}\"".format(tag_name)])
        assert len(find_result) == 2
        for item in find_result:
            assert versions[item._msg.version]
            msg_other = versions[item._msg.version]._msg
            item._msg.time_updated = msg_other.time_updated = 0
            labels1 = set(item._msg.labels)
            item._msg.labels[:] = []
            labels2 = set(msg_other.labels)
            msg_other.labels[:] = []
            msg_other.model.CopyFrom(item._msg.model)
            assert labels1 == labels2
            assert item._msg == msg_other

    def test_archive(self, model_version):
        assert (not model_version.is_archived)

        model_version.archive()
        assert model_version.is_archived

        with pytest.raises(RuntimeError) as excinfo:
            model_version.archive()

        assert "the version has already been archived" in str(excinfo.value)

    def test_clear_cache(self, registered_model):
        # Multiple log_artifacts calls, which would potentially fail without clear_cache
        model_version = registered_model.get_or_create_version(name="my version")
        model_version_2 = registered_model.get_version(id=model_version.id) # same version object

        np = pytest.importorskip("numpy")
        artifact = np.random.random((36, 12))

        for i in range(2):
            model_version.log_artifact("artifact_{}".format(2 * i), artifact)
            model_version_2.log_artifact("artifact_{}".format(2 * i + 1), artifact)

        model_version = registered_model.get_version(id=model_version.id) # re-retrieve the version
        assert len(model_version._msg.artifacts) == 4

    @pytest.mark.skip(reason="pending backend")
    def test_download_docker_context(self, experiment_run, model_for_deployment, in_tempdir, registered_model):
        download_to_path = "context.tgz"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])
        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )

        filepath = model_version.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, 'r:gz') as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths
