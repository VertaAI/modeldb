import pytest
import requests

import verta
from verta.registry import task_type as task_type_module
from verta.registry import data_type as data_type_module

from ..utils import sorted_subclasses

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestModel:
    def test_create(self, client, created_entities):
        registered_model = client.set_registered_model()
        assert registered_model
        created_entities.append(registered_model)

        name = verta._internal_utils._utils.generate_default_name()
        registered_model = client.create_registered_model(name)
        assert registered_model
        created_entities.append(registered_model)
        with pytest.raises(requests.HTTPError) as excinfo:
            assert client.create_registered_model(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value
        with pytest.warns(UserWarning, match=".*already exists.*"):
            client.set_registered_model(
                name=registered_model.name, desc="new description"
            )

    def test_get(self, client, created_entities):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_registered_model(name)

        registered_model = client.set_registered_model(name)
        created_entities.append(registered_model)

        assert (
            registered_model.id == client.get_registered_model(registered_model.name).id
        )
        assert (
            registered_model.id
            == client.get_registered_model(id=registered_model.id).id
        )

    def test_get_by_name(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)

        dummy_model = (
            client.set_registered_model()
        )  # in case get erroneously fetches latest
        created_entities.append(dummy_model)

        assert (
            registered_model.id == client.set_registered_model(registered_model.name).id
        )

    def test_get_by_id(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)

        dummy_model = (
            client.set_registered_model()
        )  # in case get erroneously fetches latest
        created_entities.append(dummy_model)

        assert (
            registered_model.id
            == client.set_registered_model(id=registered_model.id).id
        )

    def test_repr(self, registered_model):
        registered_model.add_labels(["tag1", "tag2"])
        repr = str(registered_model)

        assert registered_model.name in repr
        assert registered_model.url in repr
        assert str(registered_model.id) in repr
        assert str(registered_model.get_labels()) in repr

    def test_find(self, client, created_entities):
        name = verta._internal_utils._utils.generate_default_name()
        registered_model = client.set_registered_model(name)
        created_entities.append(registered_model)

        find = client.registered_models.find(["name == '{}'".format(name)])
        assert len(find) == 1
        for item in find:
            assert item._msg == registered_model._msg

        tag_name = name + "_new_tag"
        registered_models = {
            name
            + "1": client.set_registered_model(name + "1", labels=[tag_name, "tag2"]),
            name + "2": client.set_registered_model(name + "2", labels=[tag_name]),
        }
        created_entities.extend(registered_models.values())
        find = client.registered_models.find(['labels == "{}"'.format(tag_name)])
        assert len(find) == 2
        for item in find:
            assert item._msg == registered_models[item._msg.name]._msg

    def test_labels(self, client, created_entities):
        registered_model = client.set_registered_model(labels=["tag1", "tag2"])
        assert registered_model
        created_entities.append(registered_model)

        assert registered_model is not None
        registered_model.add_label("tag3")
        assert registered_model.get_labels() == ["tag1", "tag2", "tag3"]
        registered_model.del_label("tag2")
        assert registered_model.get_labels() == ["tag1", "tag3"]
        registered_model.del_label("tag4")
        assert registered_model.get_labels() == ["tag1", "tag3"]
        registered_model.add_label("tag2")
        assert registered_model.get_labels() == ["tag1", "tag2", "tag3"]

        registered_model.add_labels(
            ["tag2", "tag4", "tag1", "tag5"]
        )  # some tags already exist
        assert registered_model.get_labels() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

    def test_description(self, client, created_entities):
        desc = "description"
        registered_model = client.get_or_create_registered_model()
        created_entities.append(registered_model)
        registered_model.set_description(desc)
        assert desc == registered_model.get_description()

    def test_pii(self, client, created_entities):
        # Test setting on creation
        registered_model = client.get_or_create_registered_model(pii=True)
        created_entities.append(registered_model)
        assert registered_model.get_pii() is True
        registered_model = client.create_registered_model(pii=True)
        created_entities.append(registered_model)
        assert registered_model.get_pii() is True

        # Test updating
        registered_model = client.get_or_create_registered_model()
        created_entities.append(registered_model)
        registered_model.set_pii(True)
        assert registered_model.get_pii() is True
        registered_model.set_pii(False)
        assert registered_model.get_pii() is False



class TestTaskTypes:
    @pytest.mark.parametrize(
        "task_type_cls", sorted_subclasses(task_type_module._TaskType)
    )
    def test_creation(self, client, created_entities, task_type_cls):
        if task_type_cls is task_type_module._Unknown:
            pytest.skip("unsupported task type")

        task_type = task_type_cls()

        registered_model = client.create_registered_model(task_type=task_type)
        created_entities.append(registered_model)

        registered_model = client.get_registered_model(id=registered_model.id)
        assert registered_model.get_task_type() == task_type

    @pytest.mark.parametrize(
        "task_type_cls", sorted_subclasses(task_type_module._TaskType)
    )
    def test_set(self, registered_model, task_type_cls):
        if task_type_cls is task_type_module._Unknown:
            pytest.skip("unsupported task type")

        task_type = task_type_cls()

        registered_model.set_task_type(task_type)

        assert registered_model.get_task_type() == task_type


class TestDataTypes:
    @pytest.mark.parametrize(
        "data_type_cls", sorted_subclasses(data_type_module._DataType)
    )
    def test_creation(self, client, created_entities, data_type_cls):
        if data_type_cls is data_type_module._Unknown:
            pytest.skip("unsupported data type")

        data_type = data_type_cls()

        registered_model = client.create_registered_model(data_type=data_type)
        created_entities.append(registered_model)

        registered_model = client.get_registered_model(id=registered_model.id)
        assert registered_model.get_data_type() == data_type

    @pytest.mark.parametrize(
        "data_type_cls", sorted_subclasses(data_type_module._DataType)
    )
    def test_set(self, registered_model, data_type_cls):
        if data_type_cls is data_type_module._Unknown:
            pytest.skip("unsupported data type")

        data_type = data_type_cls()

        registered_model.set_data_type(data_type)

        assert registered_model.get_data_type() == data_type
