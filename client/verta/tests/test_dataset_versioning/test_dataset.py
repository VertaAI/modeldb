import verta
import pytest
import requests


class TestDataset:
    def test_description(self, client, created_datasets):
        original_description = "this is a cool dataset"
        dataset = client._set_dataset2(desc=original_description)
        created_datasets.append(dataset)
        assert dataset.get_description() == original_description

        updated_description = "this is an uncool dataset"
        dataset.set_description(updated_description)
        assert dataset.get_description() == updated_description

        assert client._get_dataset2(id=dataset.id).get_description() == updated_description

    def test_create(self, client, created_datasets):
        dataset = client._set_dataset2()
        assert dataset
        created_datasets.append(dataset)

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._create_dataset2(name)
        assert dataset
        created_datasets.append(dataset)

        with pytest.raises(requests.HTTPError) as excinfo:
            assert client._create_dataset2(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client._set_dataset2(name=dataset.name, time_created=123)

    def test_get(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client._get_dataset2(name)

        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)

        assert dataset.id == client._get_dataset2(dataset.name).id
        assert dataset.id == client._get_dataset2(id=dataset.id).id

    def test_attributes(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name, attrs={"string-attr": "some-attr", "int-attr": 12, "bool-attr": False})
        created_datasets.append(dataset)
        assert dataset.get_attributes() == {"string-attr": "some-attr", "int-attr": 12, "bool-attr": False}

        dataset.add_attribute("float-attr", 0.4)
        assert dataset.get_attribute("float-attr") == 0.4

        # Test overwriting
        dataset.add_attribute("int-attr", 15)
        assert dataset.get_attribute("int-attr") == 15

        # Test deleting:
        dataset.del_attribute('string-attr')
        assert dataset.get_attributes() ==  {"int-attr": 15, "bool-attr": False, "float-attr": 0.4}

        # Deleting non-existing key:
        dataset.del_attribute("non-existing")

    def test_tags(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name, tags=["tag1", "tag2"])
        created_datasets.append(dataset)
        assert dataset.get_tags() == ["tag1", "tag2"]

        dataset.add_tag("tag3")
        assert dataset.get_tags() == ["tag1", "tag2", "tag3"]

        dataset.add_tags(["tag1", "tag4", "tag5"])
        assert dataset.get_tags() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

        dataset.del_tag("tag2")
        assert dataset.get_tags() == ["tag1", "tag3", "tag4", "tag5"]

        dataset.del_tag("tag100") # delete non-existing tag does not error out

    def test_repr(self, client, created_datasets):
        description = "this is a cool dataset"
        tags = [u"tag1", u"tag2"]
        dataset = client._set_dataset2(desc=description, tags=tags)
        created_datasets.append(dataset)

        str_repr = repr(dataset)

        assert "name: {}".format(dataset.name) in str_repr
        assert "id: {}".format(dataset.id) in str_repr
        assert "time created" in str_repr
        assert "time updated" in str_repr
        assert "description: {}".format(description) in str_repr
        assert "tags: {}".format(tags) in str_repr
