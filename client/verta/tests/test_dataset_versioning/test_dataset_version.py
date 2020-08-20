import verta

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

