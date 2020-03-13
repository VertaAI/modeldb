import pytest

import verta.dataset


class TestS3:
    def test_s3_bucket(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3("s3://verta-starter")
        assert len(dataset._msg.s3.components) > 1

        for s3_obj_metadata in (component.path for component in dataset._msg.s3.components):
            assert s3_obj_metadata.path != ""
            assert s3_obj_metadata.size != 0
            assert s3_obj_metadata.last_modified_at_source != 0
            assert s3_obj_metadata.md5 != ""

    def test_s3_key(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3("s3://verta-starter/census-test.csv")

        assert len(dataset._msg.s3.components) == 1

        s3_obj_metadata = dataset._msg.s3.components[0].path
        assert s3_obj_metadata.path != ""
        assert s3_obj_metadata.size != 0
        assert s3_obj_metadata.last_modified_at_source != 0
        assert s3_obj_metadata.md5 != ""

    def test_s3_multiple_keys(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3([
            "s3://verta-starter/census-test.csv",
            "s3://verta-starter/census-train.csv",
        ])

        assert len(dataset._msg.s3.components) == 2

        for s3_obj_metadata in (component.path for component in dataset._msg.s3.components):
            assert s3_obj_metadata.path != ""
            assert s3_obj_metadata.size != 0
            assert s3_obj_metadata.last_modified_at_source != 0
            assert s3_obj_metadata.md5 != ""

    def test_s3_no_duplicates(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        multiple_dataset = verta.dataset.S3([
            "s3://verta-starter",
            "s3://verta-starter/census-test.csv",
        ])
        bucket_dataset = verta.dataset.S3("s3://verta-starter")

        assert len(multiple_dataset._msg.s3.components) == len(bucket_dataset._msg.s3.components)


class TestPath:
    def test_dirpath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/")
        assert len(dataset._msg.path.components) > 1

        for file_metadata in dataset._msg.path.components:
            assert file_metadata.path != ""
            assert file_metadata.size != 0
            assert file_metadata.last_modified_at_source != 0
            assert file_metadata.md5 != ""

    def test_filepath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/api_generator.py")

        assert len(dataset._msg.path.components) == 1

        file_metadata = dataset._msg.path.components[0]
        assert file_metadata.path != ""
        assert file_metadata.size != 0
        assert file_metadata.last_modified_at_source != 0
        assert file_metadata.md5 != ""

    def test_multiple_filepaths(self):
        dataset = verta.dataset.Path([
            "modelapi_hypothesis/api_generator.py",
            "modelapi_hypothesis/test_modelapi.py",
        ])
        assert len(dataset._msg.path.components) == 2

        for file_metadata in dataset._msg.path.components:
            assert file_metadata.path != ""
            assert file_metadata.size != 0
            assert file_metadata.last_modified_at_source != 0
            assert file_metadata.md5 != ""

    def test_no_duplicates(self):
        multiple_dataset = verta.dataset.Path([
            "modelapi_hypothesis/",
            "modelapi_hypothesis/api_generator.py",
        ])
        dir_dataset = verta.dataset.Path("modelapi_hypothesis/")
        assert len(multiple_dataset._msg.path.components) == len(dir_dataset._msg.path.components)
