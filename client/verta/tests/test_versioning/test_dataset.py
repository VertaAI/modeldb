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

    def test_versioned_bucket(self):
        s3 = pytest.importorskip("boto3").client('s3')
        S3_PATH = verta.dataset.S3._S3_PATH

        bucket = "verta-versioned-bucket"

        # collect latest versions of objects
        version_ids = {
            S3_PATH.format(bucket, obj['Key']): obj['VersionId']
            for obj in
            s3.list_object_versions(Bucket=bucket)['Versions']
            if obj['IsLatest']
        }
        for path, version_id in version_ids.items():
            if version_id == "null":
                # S3 returns "null" in its API, but we handle that as empty string
                version_ids[path] = ""

        dataset = verta.dataset.S3("s3://{}".format(bucket))

        for component in dataset._msg.s3.components:
            assert component.s3_version_id == version_ids[component.path.path]

    def test_versioned_object(self):
        s3 = pytest.importorskip("boto3").client('s3')

        bucket = "verta-versioned-bucket"
        key = "data/census-train.csv"

        obj = s3.head_object(Bucket=bucket, Key=key)
        latest_version_id = obj['VersionId']

        dataset = verta.dataset.S3("s3://{}/{}".format(bucket, key))

        assert len(dataset._msg.s3.components) == 1
        assert dataset._msg.s3.components[0].s3_version_id == latest_version_id

    def test_versioned_object_by_id(self):
        s3 = pytest.importorskip("boto3").client('s3')

        bucket = "verta-versioned-bucket"
        key = "data/census-train.csv"
        s3_url = "s3://{}/{}".format(bucket, key)

        # pick a version that's not the latest
        version_ids = [
            obj['VersionId']
            for obj in
            s3.list_object_versions(Bucket=bucket)['Versions']
            if not obj['IsLatest']
            and obj['Key'] == key
        ]
        version_id = version_ids[0]

        s3_loc = verta.dataset._s3.S3Location(s3_url, version_id)
        dataset = verta.dataset.S3(s3_loc)

        assert len(dataset._msg.s3.components) == 1
        assert dataset._msg.s3.components[0].s3_version_id == version_id

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        pytest.importorskip("boto3")

        dataset_ver = verta.dataset.S3("s3://verta-starter")

        assert dataset_ver.__repr__()


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

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        dataset = verta.dataset.Path("modelapi_hypothesis/")

        assert dataset.__repr__()
