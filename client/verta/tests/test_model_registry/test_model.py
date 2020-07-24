import pytest
import verta


class TestModel:
    def test_create(self, client):
        assert client.set_registered_model()

        assert client.registered_model is not None

    def test_get(self, client):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_registered_model(name)

        registered_model = client.set_registered_model(name)

        assert registered_model.id == client.get_registered_model(registered_model.name).id
        assert registered_model.id == client.get_registered_model(id=registered_model.id).id

    def test_get_by_name(self, client):
        registered_model = client.set_registered_model()

        client.set_registered_model()  # in case get erroneously fetches latest

        assert registered_model.id == client.set_registered_model(registered_model.name).id

    def test_get_by_id(self, client):
        registered_model = client.set_registered_model()

        client.set_registered_model()  # in case get erroneously fetches latest

        assert registered_model.id == client.set_registered_model(id=registered_model.id).id

    def test_find(self, client):
        name = "registered_model_new_test"
        registered_model = client.set_registered_model(name)

        find = client.registered_models.find(["name == '{}'".format(name)])
        assert len(find) == 1
        for item in find:
            assert item._msg == registered_model._msg

        tag_name = name + "_new_tag"
        registered_model = {name + "1": client.set_registered_model(name + "1", labels=[tag_name, "tag2"]),
                            name + "2": client.set_registered_model(name + "2", labels=[tag_name])}
        find = client.registered_models.find(["labels == \"{}\"".format(tag_name)])
        assert len(find) == 2
        for item in find:
            assert item._msg == registered_model[item._msg.name]._msg

    def test_labels(self, client):
        assert client.set_registered_model(labels=["tag1", "tag2"])

        assert client.registered_model is not None
        client.registered_model.add_label("tag3")
        assert client.registered_model.get_labels() == ["tag1", "tag2", "tag3"]
        client.registered_model.del_label("tag2")
        assert client.registered_model.get_labels() == ["tag1", "tag3"]
        client.registered_model.del_label("tag4")
        assert client.registered_model.get_labels() == ["tag1", "tag3"]
        client.registered_model.add_label("tag2")
        assert client.registered_model.get_labels() == ["tag1", "tag2", "tag3"]

        client.registered_model.add_labels(["tag2", "tag4", "tag1", "tag5"]) # some tags already exist
        assert client.registered_model.get_labels() == ["tag1", "tag2", "tag3", "tag4", "tag5"]
