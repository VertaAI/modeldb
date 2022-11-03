# -*- coding: utf-8 -*-

import pytest

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module

class TestLogDataset:
    def test_log_dataset(self, model_version):
        key1, key2, key3 = "version1", "version2", "version3"

        model_version.log_dataset(
            key=key1,
            path="ModelVersionEntity/191/train",
            path_only=True,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.DATA,
            linked_artifact_id="fbcbd389db61d78c479634b0824cf063ffc63a6af7ba5d763388a42001ed6200",
        )

        model_version.log_dataset(
            key=key2,
            path="ModelVersionEntity/192/train",
            path_only=True,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.BLOB,
            linked_artifact_id="fbcbd389db61d78c479634b0824cf063ffc63a6af7ba5d763388a42001ed6200",
        )

        model_version.log_dataset(
            key=key3,
            path="ModelVersionEntity/193/train",
            path_only=True,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.MODEL,
            linked_artifact_id="fbcbd389db61d78c479634b0824cf063ffc63a6af7ba5d763388a42001ed6200",
        )

        keys = model_version.get_dataset_keys()

        assert len(keys) == 3
        assert key1 in keys
        assert key2 in keys
        assert key3 in keys

        datasets = model_version.get_datasets()
        assert len(datasets) == 3

        with pytest.raises(KeyError) as excinfo:
            model_version.get_dataset("fake")

        assert "no dataset found with key" in str(excinfo.value)

        dataset = model_version.get_dataset(key1)
        assert dataset.artifact_type == _CommonCommonService.ArtifactTypeEnum.DATA

        dataset = model_version.get_dataset(key2)
        assert dataset.artifact_type == _CommonCommonService.ArtifactTypeEnum.BLOB

        dataset = model_version.get_dataset(key3)
        assert dataset.artifact_type == _CommonCommonService.ArtifactTypeEnum.MODEL
