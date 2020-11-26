import collections

import pytest

import verta
from verta.dataset import Path, S3


class TestMetadata:  # essentially copied from test_dataset.py
    def test_description(self, client, created_datasets, strs):
        first_desc, second_desc = strs[:2]
        dataset = client.create_dataset()
        created_datasets.append(dataset)

        version = dataset.create_version(Path("conftest.py"), desc=first_desc)
        assert version.get_description() == first_desc

        version.set_description(second_desc)
        assert version.get_description() == second_desc

        assert dataset.get_version(id=version.id).get_description() == second_desc

    def test_tags(self, client, created_datasets, strs):
        tag1, tag2, tag3, tag4 = strs[:4]
        dataset = client.create_dataset()
        created_datasets.append(dataset)

        version = dataset.create_version(Path("conftest.py"), tags=[tag1])
        assert set(version.get_tags()) == {tag1}

        version.add_tag(tag2)
        assert set(version.get_tags()) == {tag1, tag2}

        version.add_tags([tag3, tag4])
        assert set(version.get_tags()) == {tag1, tag2, tag3, tag4}

        version.del_tag(tag3)
        version.del_tag(tag3)  # no error if nonexistent
        assert set(version.get_tags()) == {tag1, tag2, tag4}

        assert set(dataset.get_version(id=version.id).get_tags()) == {tag1, tag2, tag4}

    def test_attributes(self, client, created_datasets):
        Attr = collections.namedtuple('Attr', ['key', 'value'])
        attr1 = Attr('key1', {'a': 1})
        attr2 = Attr('key2', ['a', 1])
        attr3 = Attr('key3', 'a')
        attr4 = Attr('key4', 1)
        dataset = client.create_dataset()
        created_datasets.append(dataset)

        version = dataset.create_version(Path("conftest.py"), attrs=dict([attr1]))
        assert version.get_attributes() == dict([attr1])

        version.add_attribute(*attr2)
        assert version.get_attributes() == dict([attr1, attr2])

        version.add_attributes(dict([attr3, attr4]))
        assert version.get_attributes() == dict([attr1, attr2, attr3, attr4])

        version.del_attribute(attr3.key)
        version.del_attribute(attr3.key)  # no error if nonexistent
        assert version.get_attributes() == dict([attr1, attr2, attr4])

        assert dataset.get_version(id=version.id).get_attributes() == dict([attr1, attr2, attr4])

        for attr in [attr1, attr2, attr4]:
            assert version.get_attribute(attr.key) == attr.value

        # overwrite
        new_val = 'b'
        dataset.add_attribute(attr1.key, new_val)
        assert dataset.get_attribute(attr1.key) == new_val

    def test_dataset_and_parent_ids(self, client, created_datasets, with_boto3):
        dataset = client.create_dataset()
        created_datasets.append(dataset)

        version1 = dataset.create_version(Path("conftest.py"))
        version2 = dataset.create_version(S3("s3://verta-starter/census-train.csv"))

        assert version1.dataset_id == version2.dataset_id == dataset.id
        assert version2.parent_id == version1.id


class TestCreateGet:
    def test_create_get_path(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.set_dataset(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_version(Path(["modelapi_hypothesis/"]))
        dataset_version2 = dataset.create_version(Path(["modelapi_hypothesis/api_generator.py"]))

        assert dataset_version.id == client.get_dataset_version(dataset_version.id).id
        assert dataset_version2.id == client.get_dataset_version(id=dataset_version2.id).id
        assert dataset_version2.id == dataset.get_latest_version().id

    def test_get_latest_printing(self, client, created_datasets, capsys):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.set_dataset(name)
        created_datasets.append(dataset)

        dataset_version = dataset.create_version(Path(["modelapi_hypothesis/"]))
        dataset.get_latest_version()

        captured = capsys.readouterr()
        assert "got existing dataset version: {}".format(dataset_version.id) in captured.out

    def test_create_get_s3(self, client, created_datasets, with_boto3):
        pytest.importorskip("boto3")

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.set_dataset(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_version(S3(["s3://verta-starter/", "s3://verta-starter/census-test.csv"]))

        assert dataset_version.id == dataset.get_version(id=dataset_version.id).id

    def test_find(self, client, created_datasets, strs):  # essentially copied from test_dataset.py
        tag1, tag2, tag3 = strs[:3]
        dataset = client.create_dataset()
        created_datasets.append(dataset)

        version1 = dataset.create_version(Path("conftest.py"), tags=[tag1])
        version2 = dataset.create_version(Path("conftest.py"), tags=[tag1])
        version3 = dataset.create_version(Path("conftest.py"), tags=[tag1, tag2])
        version4 = dataset.create_version(Path("conftest.py"), tags=[      tag2, tag3])

        versions = dataset.versions.find("tags ~= {}".format(tag1))
        assert len(versions) == 3
        assert set(version.id for version in versions) == {version1.id, version2.id, version3.id}

        versions = dataset.versions.find("tags ~= {}".format(tag2))
        assert len(versions) == 2
        assert set(version.id for version in versions) == {version3.id, version4.id}

        versions = dataset.versions.find("tags ~= {}".format(tag3))
        assert len(versions) == 1
        assert set(version.id for version in versions) == {version4.id}

    def test_repr(self, client, created_datasets):  # essentially copied from test_dataset.py
        description = "this is a cool version"
        tags = [u"tag1", u"tag2"]
        dataset = client.create_dataset()
        created_datasets.append(dataset)
        version = dataset.create_version(Path("conftest.py"), desc=description, tags=tags)

        str_repr = repr(version)

        assert "version: {}".format(version.version) in str_repr
        assert "id: {}".format(version.id) in str_repr
        assert "time created" in str_repr
        assert "time updated" in str_repr
        assert "description: {}".format(description) in str_repr
        assert "tags: {}".format(tags) in str_repr

        # check content
        content = version.get_content()
        for repr_line in repr(content).splitlines():
            assert repr_line in str_repr.split("content:")[-1]


class TestLogging:
    def test_log_get(self, client, experiment_run, created_datasets, strs, with_boto3):
        """Tests ExperimentRun.log_dataset_version() and ExperimentRun.get_dataset_version()."""
        key1, key2 = strs[:2]
        dataset = client.create_dataset()
        version1 = dataset.create_version(Path("conftest.py"))
        version2 = dataset.create_version(S3("s3://verta-starter/census-train.csv"))

        experiment_run.log_dataset_version(key1, version1)
        experiment_run.log_dataset_version(key2, version2)

        for key, version in [(key1, version1), (key2, version2)]:
            retrieved_version = experiment_run.get_dataset_version(key)
            assert retrieved_version.id == version.id

            retrieved_components = sorted(retrieved_version.get_content().list_components(), key=lambda component: component.path)
            components          = sorted(version.get_content().list_components(),           key=lambda component: component.path)
            assert retrieved_components == components
