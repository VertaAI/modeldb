import collections
import os

import pytest

import verta
from verta.dataset import Path, S3


class TestMetadata:  # essentially copied from test_dataset.py
    def test_description(self, client, dataset, strs):
        first_desc, second_desc = strs[:2]

        version = dataset.create_version(Path("conftest.py"), desc=first_desc)
        assert version.get_description() == first_desc

        version.set_description(second_desc)
        assert version.get_description() == second_desc

        assert dataset.get_version(id=version.id).get_description() == second_desc

    def test_tags(self, client, dataset, strs):
        tag1, tag2, tag3, tag4 = strs[:4]

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

    def test_attributes(self, client, dataset):
        Attr = collections.namedtuple('Attr', ['key', 'value'])
        attr1 = Attr('key1', {'a': 1})
        attr2 = Attr('key2', ['a', 1])
        attr3 = Attr('key3', 'a')
        attr4 = Attr('key4', 1)

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

    def test_dataset_and_parent_ids(self, client, dataset, with_boto3):
        version1 = dataset.create_version(Path("conftest.py"))
        version2 = dataset.create_version(S3("s3://verta-starter/census-train.csv"))

        assert version1.dataset_id == version2.dataset_id == dataset.id
        assert version2.parent_id == version1.id


class TestCreateGet:
    def test_create_get_path(self, client, dataset):
        dataset_version = dataset.create_version(Path(["modelapi_hypothesis/"]))
        dataset_version2 = dataset.create_version(Path(["modelapi_hypothesis/api_generator.py"]))

        assert dataset_version.id == client.get_dataset_version(dataset_version.id).id
        assert dataset_version2.id == client.get_dataset_version(id=dataset_version2.id).id
        assert dataset_version2.id == dataset.get_latest_version().id

    def test_get_latest_printing(self, client, dataset, capsys):
        dataset_version = dataset.create_version(Path(["modelapi_hypothesis/"]))
        dataset.get_latest_version()

        captured = capsys.readouterr()
        assert "got existing dataset version: {}".format(dataset_version.id) in captured.out

    def test_create_get_s3(self, client, dataset, with_boto3):
        pytest.importorskip("boto3")

        dataset_version = dataset.create_version(S3(["s3://verta-starter/", "s3://verta-starter/census-test.csv"]))

        assert dataset_version.id == dataset.get_version(id=dataset_version.id).id

    def test_find(self, client, dataset, strs):  # essentially copied from test_dataset.py
        tag1, tag2, tag3 = strs[:3]

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

    def test_repr(self, client, dataset):  # essentially copied from test_dataset.py
        description = "this is a cool version"
        tags = [u"tag1", u"tag2"]
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
    def test_log_get(self, client, experiment_run, dataset, strs, with_boto3):
        """Tests ExperimentRun.log_dataset_version() and ExperimentRun.get_dataset_version()."""
        key1, key2 = strs[:2]
        version1 = dataset.create_version(Path("conftest.py"))
        version2 = dataset.create_version(S3("s3://verta-starter/census-train.csv"))

        experiment_run.log_dataset_version(key1, version1)
        experiment_run.log_dataset_version(key2, version2)

        for key, version in [(key1, version1), (key2, version2)]:
            retrieved_version = experiment_run.get_dataset_version(key)
            assert retrieved_version.id == version.id

            retrieved_components = sorted(
                retrieved_version.get_content().list_components(),
                key=lambda component: component.path,
            )
            components = sorted(
                version.get_content().list_components(),
                key=lambda component: component.path,
            )
            assert retrieved_components == components


@pytest.mark.usefixtures("in_tempdir")
class TestManagedVersioning:
    def test_path(self, client, dataset):
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        content = Path(filename, enable_mdb_versioning=True)
        version = dataset.create_version(content)

        downloaded_filename = version.get_content().download(filename)
        with open(downloaded_filename, 'rb') as f:
            assert f.read() == FILE_CONTENTS

    def test_s3(self, client, dataset):
        s3 = pytest.importorskip("boto3").client('s3')

        filename = "tiny1.bin"
        bucket = "verta-versioned-bucket"
        key = "tiny-files/{}".format(filename)
        s3_key = "s3://{}/{}".format(bucket, key)

        # get file contents directly from S3 for reference
        s3.download_file(bucket, key, filename)
        with open(filename, 'rb') as f:
            FILE_CONTENTS = f.read()
        os.remove(filename)

        content = S3(s3_key, enable_mdb_versioning=True)
        version = dataset.create_version(content)

        downloaded_filename = version.get_content().download(s3_key)
        with open(downloaded_filename, 'rb') as f:
            assert f.read() == FILE_CONTENTS
