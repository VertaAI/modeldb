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

        with pytest.raises(KeyError) as excinfo:
            model_version.get_dataset_version("fake")

        assert "no dataset found with key" in str(excinfo.value)

        dataset = model_version.get_dataset_version(key1)
        assert dataset_version1.id in dataset.linked_artifact_id

        dataset = model_version.get_dataset_version(key2)
        #assert dataset.artifact_type == _CommonCommonService.ArtifactTypeEnum.BLOB
        #assert "fbcbd389db61d78c479634b0824cf063ffc63a6af7ba5d763388a42001ed6192" in dataset.linked_artifact_id

        dataset = model_version.get_dataset_version(key3)
        #assert dataset.artifact_type == _CommonCommonService.ArtifactTypeEnum.MODEL
        #assert "fbcbd389db61d78c479634b0824cf063ffc63a6af7ba5d763388a42001ed6193" in dataset.linked_artifact_id
