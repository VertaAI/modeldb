import verta
import pytest
import requests


class TestDatasetVersion:
    def test_create_get(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client._set_dataset2(name)
        created_datasets.append(dataset)
        dataset_version = dataset.create_s3_version(paths=["s3://verta-starter/", "s3://verta-starter/census-test.csv"])
        dataset_version2 = dataset.create_path_version(paths=["modelapi_hypothesis/",
                                                              "modelapi_hypothesis/api_generator.py"])

        assert dataset_version.id == client._get_dataset_version2(dataset_version.id).id
        assert dataset_version2.id == client._get_dataset_version2(id=dataset_version2.id).id
        assert dataset_version2.id == dataset.get_latest_version().id
        
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

