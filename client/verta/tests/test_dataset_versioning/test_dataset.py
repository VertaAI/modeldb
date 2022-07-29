import collections
import time

import pytest
import requests

import verta
from verta.dataset import Path


class TestMetadata:
    def test_description(self, client, created_entities, strs):
        first_desc, second_desc = strs[:2]

        dataset = client.create_dataset(desc=first_desc)
        created_entities.append(dataset)
        assert dataset.get_description() == first_desc

        dataset.set_description(second_desc)
        assert dataset.get_description() == second_desc

        assert client.get_dataset(id=dataset.id).get_description() == second_desc

    def test_tags(self, client, created_entities, strs):
        tag1, tag2, tag3, tag4 = strs[:4]

        dataset = client.create_dataset(tags=[tag1])
        created_entities.append(dataset)
        assert set(dataset.get_tags()) == {tag1}

        dataset.add_tag(tag2)
        assert set(dataset.get_tags()) == {tag1, tag2}

        dataset.add_tags([tag3, tag4])
        assert set(dataset.get_tags()) == {tag1, tag2, tag3, tag4}

        dataset.del_tag(tag3)
        dataset.del_tag(tag3)  # no error if nonexistent
        assert set(dataset.get_tags()) == {tag1, tag2, tag4}

        assert set(client.get_dataset(id=dataset.id).get_tags()) == {tag1, tag2, tag4}

    def test_attributes(self, client, created_entities):
        Attr = collections.namedtuple('Attr', ['key', 'value'])
        attr1 = Attr('key1', {'a': 1})
        attr2 = Attr('key2', ['a', 1])
        attr3 = Attr('key3', 'a')
        attr4 = Attr('key4', 1)

        dataset = client.create_dataset(attrs=dict([attr1]))
        created_entities.append(dataset)
        assert dataset.get_attributes() == dict([attr1])

        dataset.add_attribute(*attr2)
        assert dataset.get_attributes() == dict([attr1, attr2])

        dataset.add_attributes(dict([attr3, attr4]))
        assert dataset.get_attributes() == dict([attr1, attr2, attr3, attr4])

        dataset.del_attribute(attr3.key)
        dataset.del_attribute(attr3.key)  # no error if nonexistent
        assert dataset.get_attributes() == dict([attr1, attr2, attr4])

        assert client.get_dataset(id=dataset.id).get_attributes() == dict([attr1, attr2, attr4])

        for attr in [attr1, attr2, attr4]:
            assert dataset.get_attribute(attr.key) == attr.value

        # overwrite
        new_val = 'b'
        dataset.add_attribute(attr1.key, new_val)
        assert dataset.get_attribute(attr1.key) == new_val


class TestCreateGet:
    def test_creation_updates_dataset_timestamp(self, client, dataset):
        """Version creation should update its dataset's time_updated field."""
        time_updated = dataset._msg.time_updated

        dataset_version = dataset.create_version(Path(["conftest.py"]))

        time.sleep(60)  # wait for reconciler
        dataset._fetch_with_no_cache()
        assert dataset._msg.time_updated > time_updated
        assert dataset._msg.time_updated == dataset_version._msg.time_logged

    def test_create(self, client, created_entities):
        dataset = client.set_dataset()
        assert dataset
        created_entities.append(dataset)

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.create_dataset(name)
        assert dataset
        created_entities.append(dataset)

        with pytest.raises(requests.HTTPError, match="already exists"):
            assert client.create_dataset(name)
        with pytest.warns(UserWarning, match="already exists"):
            client.set_dataset(name=dataset.name, time_created=123)

    def test_get(self, client, created_entities):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_dataset(name)

        dataset = client.set_dataset(name)
        created_entities.append(dataset)

        assert dataset.id == client.get_dataset(dataset.name).id
        assert dataset.id == client.get_dataset(id=dataset.id).id

        # Deleting non-existing key:
        dataset.del_attribute("non-existing")

    def test_find(self, client, created_entities, strs):
        name1, name2, name3, name4, tag1, tag2 = (
            s + str(verta._internal_utils._utils.now())
            for s in strs[:6]
        )

        dataset1 = client.create_dataset(name1, tags=[tag1])
        dataset2 = client.create_dataset(name2, tags=[tag1])
        dataset3 = client.create_dataset(name3, tags=[tag2])
        dataset4 = client.create_dataset(name4, tags=[tag2])
        created_entities.extend([dataset1, dataset2, dataset3, dataset4])

        datasets = client.datasets.find("name == {}".format(name3))
        assert len(datasets) == 1
        assert datasets[0].id == dataset3.id

        datasets = client.datasets.find("tags ~= {}".format(tag1))
        assert len(datasets) == 2
        assert set(dataset.id for dataset in datasets) == {dataset1.id, dataset2.id}

    def test_repr(self, client, created_entities):
        description = "this is a cool dataset"
        tags = [u"tag1", u"tag2"]
        dataset = client.set_dataset(desc=description, tags=tags)
        created_entities.append(dataset)

        str_repr = repr(dataset)

        assert "name: {}".format(dataset.name) in str_repr
        assert "id: {}".format(dataset.id) in str_repr
        assert "time created" in str_repr
        assert "time updated" in str_repr
        assert "description: {}".format(description) in str_repr
        assert "tags: {}".format(tags) in str_repr
