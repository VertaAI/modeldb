# -*- coding: utf-8 -*-

import pytest

from verta.dataset import Path

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module

class TestLogDataset:
    def test_log_dataset(self, client, model_version, dataset):
        key1, key2, key3 = "version1", "version2", "version3"

        dataset_version1 = dataset.create_version(Path("conftest.py"))
        dataset_version2 = dataset.create_version(Path(["modelapi_hypothesis/"]))
        dataset_version3 = dataset.create_version(Path(["modelapi_hypothesis/"]))

        model_version.log_dataset_version(key=key1, dataset_version=dataset_version1)
        model_version.log_dataset_version(key=key2, dataset_version=dataset_version2)
        model_version.log_dataset_version(key=key3, dataset_version=dataset_version3)

        model_version = client.get_registered_model_version(id=model_version.id)

        with pytest.raises(KeyError, match="no dataset found with key"):
            model_version.get_dataset_version("fake")

        dataset = model_version.get_dataset_version(key1)
        assert dataset_version1.id == dataset.id

        dataset = model_version.get_dataset_version(key2)
        assert dataset_version2.id == dataset.id

        dataset = model_version.get_dataset_version(key3)
        assert dataset_version3.id == dataset.id

        dataset_versions = model_version.get_dataset_versions()
        assert dataset_versions[0].id == dataset_version1.id
        assert dataset_versions[1].id == dataset_version2.id
        assert dataset_versions[2].id == dataset_version3.id

        with pytest.raises(KeyError, match="no dataset found with key"):
            model_version.del_dataset_version("fake")

        model_version.del_dataset_version(key3)

        with pytest.raises(KeyError, match="no dataset found with key"):
            model_version.get_dataset_version(key3)
