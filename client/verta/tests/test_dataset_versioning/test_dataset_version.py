import pytest

import verta


class TestDatasetVersion:
    def test_create_get_path(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_path_version(paths=["modelapi_hypothesis/"])
        dataset_version2 = dataset.create_path_version(paths=["modelapi_hypothesis/api_generator.py"])

        assert dataset_version.id == client._get_dataset_version2(dataset_version.id).id
        assert dataset_version2.id == client._get_dataset_version2(id=dataset_version2.id).id
        assert dataset_version2.id == dataset.get_latest_version().id

    def test_get_latest_printing(self, client, created_datasets, capsys):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)

        dataset_version = dataset.create_path_version(paths=["modelapi_hypothesis/"])
        dataset.get_latest_version()

        captured = capsys.readouterr()
        assert "got existing dataset version: {}".format(dataset_version.id) in captured.out

    def test_create_get_s3(self, client, created_datasets):
        pytest.importorskip("boto3")

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_s3_version(paths=["s3://verta-starter/", "s3://verta-starter/census-test.csv"])

        assert dataset_version.id == client._get_dataset_version2(dataset_version.id).id

    def test_description(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)
        original_description = "original description"
        dataset_version = dataset.create_path_version(paths=["modelapi_hypothesis/",
                                                             "modelapi_hypothesis/api_generator.py"],
                                                              desc=original_description)
        assert dataset_version.get_description() == original_description

        updated_description = "updated description"
        dataset_version.set_description(updated_description)
        assert dataset_version.get_description() == updated_description

        assert client._get_dataset_version2(id=dataset_version.id).get_description() == updated_description

    def test_tags(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        dataset_version = dataset.create_path_version(paths=["modelapi_hypothesis/",
                                                             "modelapi_hypothesis/api_generator.py"],
                                                      tags=["tag1", "tag2"])
        created_datasets.append(dataset)
        assert dataset_version.get_tags() == ["tag1", "tag2"]

        dataset_version.add_tag("tag3")
        assert dataset_version.get_tags() == ["tag1", "tag2", "tag3"]

        dataset_version.add_tags(["tag1", "tag4", "tag5"])
        assert dataset_version.get_tags() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

        dataset_version.del_tag("tag2")
        assert dataset_version.get_tags() == ["tag1", "tag3", "tag4", "tag5"]

        dataset_version.del_tag("tag100") # delete non-existing tag does not error out

    def test_attributes(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_path_version(paths=["modelapi_hypothesis/",
                                                             "modelapi_hypothesis/api_generator.py"],
                                                      attrs={"string-attr": "some-attr",
                                                             "int-attr": 12, "bool-attr": False})
        assert dataset_version.get_attributes() == {"string-attr": "some-attr", "int-attr": 12,
                                                    "bool-attr": False}

        dataset_version.add_attribute("float-attr", 0.4)
        assert dataset_version.get_attribute("float-attr") == 0.4

        # Test overwriting
        # TODO: add overwrite test when it is enabled on backend
        # dataset_version.add_attribute("int-attr", 15)
        # assert dataset_version.get_attribute("int-attr") == 15

        # Test deleting:
        dataset_version.del_attribute('string-attr')
        assert dataset_version.get_attributes() == {"int-attr": 12, "bool-attr": False,
                                                    "float-attr": 0.4}

        # Deleting non-existing key:
        dataset_version.del_attribute("non-existing")
