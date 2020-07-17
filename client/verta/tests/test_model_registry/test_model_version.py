import pytest

from .. import utils

import verta.dataset
from verta.environment import Python


pytest.skip("registry not yet available in backend", allow_module_level=True)


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

    def test_labels(self, client):
        registered_model = client.set_registered_model()
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
