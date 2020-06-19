import os
import shutil

import pytest

import verta.dataset


class TestS3:
    def test_s3_bucket(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3("s3://verta-starter")
        assert len(dataset._path_component_blobs) > 1

        for s3_obj_metadata in dataset._path_component_blobs:
            assert s3_obj_metadata.path != ""
            assert s3_obj_metadata.size != 0
            assert s3_obj_metadata.last_modified_at_source != 0
            assert s3_obj_metadata.md5 != ""

    def test_s3_key(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3("s3://verta-starter/census-test.csv")

        assert len(dataset._path_component_blobs) == 1

        s3_obj_metadata = dataset._path_component_blobs[0]
        assert s3_obj_metadata.path != ""
        assert s3_obj_metadata.size != 0
        assert s3_obj_metadata.last_modified_at_source != 0
        assert s3_obj_metadata.md5 != ""

    def test_nonexistent_s3_folder_error(self, strs):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        with pytest.raises(ValueError) as excinfo:
            verta.dataset.S3("s3://verta-starter/{}/".format(strs[0]))
        err_msg = str(excinfo.value).strip()
        assert err_msg.startswith("folder ")
        assert err_msg.endswith(" not found")

    def test_s3_multiple_keys(self):
        # pylint: disable=no-member
        pytest.importorskip("boto3")

        dataset = verta.dataset.S3([
            "s3://verta-starter/census-test.csv",
            "s3://verta-starter/census-train.csv",
        ])

        assert len(dataset._path_component_blobs) == 2

        for s3_obj_metadata in dataset._path_component_blobs:
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

        assert len(multiple_dataset._path_component_blobs) == len(bucket_dataset._path_component_blobs)

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

    def test_versioned_folder(self):
        s3 = pytest.importorskip("boto3").client('s3')
        S3_PATH = verta.dataset.S3._S3_PATH

        bucket = "verta-versioned-bucket"
        folder = "data/"
        s3_url = "s3://{}/{}".format(bucket, folder)

        # collect latest versions of objects with folder as prefix
        version_ids = {
            S3_PATH.format(bucket, obj['Key']): obj['VersionId']
            for obj in
            s3.list_object_versions(Bucket=bucket, Prefix=folder)['Versions']
            if obj['IsLatest']
        }
        for path, version_id in version_ids.items():
            if version_id == "null":
                # S3 returns "null" in its API, but we handle that as empty string
                version_ids[path] = ""

        dataset = verta.dataset.S3(s3_url)

        for component in dataset._msg.s3.components:
            assert component.s3_version_id == version_ids[component.path.path]
            assert not component.path.path.endswith('/')
            assert component.path.size != 0

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        pytest.importorskip("boto3")

        dataset_ver = verta.dataset.S3("s3://verta-starter")

        assert dataset_ver.__repr__()

    def test_mngd_ver_file(self, commit, in_tempdir):
        filename = "tiny1.bin"
        s3_key = "s3://verta-versioned-bucket/tiny-files/{}".format(filename)
        blob_path = "data"

        dataset = verta.dataset.S3(s3_key, enable_mdb_versioning=True)

        commit.update(blob_path, dataset)
        commit.save("Version data.")
        dataset = commit.get(blob_path)

        # download to implicit path
        filepath = dataset.download(s3_key)
        assert os.path.isfile(filepath)
        assert filepath == os.path.abspath(filename)

        # download to implicit path without collision
        filepath2 = dataset.download(s3_key)
        assert os.path.isfile(filepath2)
        assert filepath2 != filepath

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(filepath)
        filepath3 = dataset.download(s3_key, filepath)
        assert filepath3 == filepath
        assert os.path.getmtime(filepath) > last_updated

    def test_mngd_ver_folder(self, commit, in_tempdir):
        dirname = "tiny-files/"
        s3_folder = "s3://verta-versioned-bucket/{}".format(dirname)
        blob_path = "data"

        dataset = verta.dataset.S3(s3_folder, enable_mdb_versioning=True)

        commit.update(blob_path, dataset)
        commit.save("Version data.")
        dataset = commit.get(blob_path)

        # download to implicit path
        dirpath = dataset.download(s3_folder)
        assert os.path.isdir(dirpath)
        assert dirpath == os.path.abspath(dirname)

        # download to implicit path without collision
        dirpath2 = dataset.download(s3_folder)
        assert os.path.isdir(dirpath2)
        assert dirpath2 != dirpath

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(dirpath)
        dirpath3 = dataset.download(s3_folder, dirpath)
        assert dirpath3 == dirpath
        assert os.path.getmtime(dirpath) > last_updated


class TestPath:
    def test_dirpath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/")
        assert len(dataset._path_component_blobs) > 1

        for file_metadata in dataset._path_component_blobs:
            assert file_metadata.path != ""
            assert file_metadata.size != 0
            assert file_metadata.last_modified_at_source != 0
            assert file_metadata.md5 != ""

    def test_filepath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/api_generator.py")

        assert len(dataset._path_component_blobs) == 1

        file_metadata = dataset._path_component_blobs[0]
        assert file_metadata.path != ""
        assert file_metadata.size != 0
        assert file_metadata.last_modified_at_source != 0
        assert file_metadata.md5 != ""

    def test_multiple_filepaths(self):
        dataset = verta.dataset.Path([
            "modelapi_hypothesis/api_generator.py",
            "modelapi_hypothesis/test_modelapi.py",
        ])
        assert len(dataset._path_component_blobs) == 2

        for file_metadata in dataset._path_component_blobs:
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
        assert len(multiple_dataset._path_component_blobs) == len(dir_dataset._path_component_blobs)

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        dataset = verta.dataset.Path("modelapi_hypothesis/")

        assert dataset.__repr__()

    def test_mngd_ver_file(self, commit, in_tempdir):
        filename = "tiny1.bin"
        with open(filename, 'wb') as f:
            f.write(os.urandom(2**16))
        blob_path = "data"

        dataset = verta.dataset.Path(filename, enable_mdb_versioning=True)

        commit.update(blob_path, dataset)
        commit.save("Version data.")
        os.remove(filename)  # delete for first download test
        dataset = commit.get(blob_path)

        # download to implicit path
        filepath = dataset.download(filename)
        assert os.path.isfile(filepath)
        assert filepath == os.path.abspath(filename)

        # download to implicit path without collision
        filepath2 = dataset.download(filename)
        assert os.path.isfile(filepath2)
        assert filepath2 != filepath

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(filepath)
        filepath3 = dataset.download(filename, filepath)
        assert filepath3 == filepath
        assert os.path.getmtime(filepath) > last_updated

    def test_mngd_ver_folder(self, commit, in_tempdir):
        dirname = "tiny-files/"
        os.mkdir(dirname)
        for i in range(3):
            with open(os.path.join(dirname, "tiny{}.bin".format(i)), 'wb') as f:
                f.write(os.urandom(2**16))
        blob_path = "data"

        dataset = verta.dataset.Path(dirname, enable_mdb_versioning=True)

        commit.update(blob_path, dataset)
        commit.save("Version data.")
        shutil.rmtree(dirname)  # delete for first download test
        dataset = commit.get(blob_path)

        # download to implicit path
        dirpath = dataset.download(dirname)
        assert os.path.isdir(dirpath)
        assert dirpath == os.path.abspath(dirname)

        # download to implicit path without collision
        dirpath2 = dataset.download(dirname)
        assert os.path.isdir(dirpath2)
        assert dirpath2 != dirpath

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(dirpath)
        dirpath3 = dataset.download(dirname, dirpath)
        assert dirpath3 == dirpath
        assert os.path.getmtime(dirpath) > last_updated
