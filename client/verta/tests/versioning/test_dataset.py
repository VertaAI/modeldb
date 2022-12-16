import itertools
import os
import shutil
import pathlib

from six.moves import filterfalse

import pytest
from .. import utils

import verta.dataset
from verta.dataset import _dataset
from verta._internal_utils import _file_utils


def assert_dirs_match(dirpath1, dirpath2):
    files1 = set(_file_utils.walk_files(dirpath1))
    files2 = set(_file_utils.walk_files(dirpath2))

    for filepath1 in files1:
        # get corresponding path in dirpath2
        relative_filepath = os.path.relpath(filepath1, dirpath1)  # drop dirpath1 prefix
        filepath2 = os.path.join(dirpath2, relative_filepath)  # rebase onto dirpath2

        assert filepath2 in files2
        files2.remove(filepath2)

        with open(filepath1, "rb") as f1:
            with open(filepath2, "rb") as f2:
                assert f1.read() == f2.read()

    assert not files2  # no additional files in `dirpath2`


@pytest.mark.usefixtures("with_boto3")
class TestS3:
    def test_s3_bucket(self):
        # pylint: disable=no-member
        dataset = verta.dataset.S3("s3://verta-starter")
        assert len(dataset.list_components()) > 1

        for component in dataset.list_components():
            assert component.path != ""
            assert component.size != 0
            assert component.last_modified != 0
            assert component.md5 != ""

    def test_s3_key(self):
        # pylint: disable=no-member
        dataset = verta.dataset.S3("s3://verta-starter/census-test.csv")

        assert len(dataset.list_components()) == 1

        component = dataset.list_components()[0]
        assert component.path != ""
        assert component.size != 0
        assert component.last_modified != 0
        assert component.md5 != ""

    def test_nonexistent_s3_folder_error(self, strs):
        # pylint: disable=no-member
        with pytest.raises(ValueError) as excinfo:
            verta.dataset.S3("s3://verta-starter/{}/".format(strs[0]))
        err_msg = str(excinfo.value).strip()
        assert err_msg.startswith("folder ")
        assert err_msg.endswith(" not found")

    def test_s3_multiple_keys(self):
        # pylint: disable=no-member
        dataset = verta.dataset.S3(
            [
                "s3://verta-starter/census-test.csv",
                "s3://verta-starter/census-train.csv",
            ]
        )

        assert len(dataset.list_components()) == 2

        for component in dataset.list_components():
            assert component.path != ""
            assert component.size != 0
            assert component.last_modified != 0
            assert component.md5 != ""

    def test_s3_no_duplicates(self):
        # pylint: disable=no-member
        multiple_dataset = verta.dataset.S3(
            [
                "s3://verta-starter",
                "s3://verta-starter/census-test.csv",
            ]
        )
        bucket_dataset = verta.dataset.S3("s3://verta-starter")

        assert len(multiple_dataset.list_components()) == len(
            bucket_dataset.list_components()
        )

    def test_versioned_bucket(self):
        s3 = pytest.importorskip("boto3").client("s3")
        S3_PATH = verta.dataset.S3._S3_PATH

        bucket = "verta-versioned-bucket"

        # collect latest versions of objects
        version_ids = {
            S3_PATH.format(bucket, obj["Key"]): obj["VersionId"]
            for obj in s3.list_object_versions(Bucket=bucket)["Versions"]
            if obj["IsLatest"]
        }
        for path, version_id in version_ids.items():
            if version_id == "null":
                # S3 returns "null" in its API, but we handle that as empty string
                version_ids[path] = ""

        dataset = verta.dataset.S3("s3://{}".format(bucket))

        for component in dataset.list_components():
            assert component.s3_version_id == version_ids[component.path]

    def test_versioned_object(self):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket = "verta-versioned-bucket"
        key = "data/census-train.csv"

        obj = s3.head_object(Bucket=bucket, Key=key)
        latest_version_id = obj["VersionId"]

        dataset = verta.dataset.S3("s3://{}/{}".format(bucket, key))

        assert len(dataset.list_components()) == 1
        assert dataset.list_components()[0].s3_version_id == latest_version_id

    def test_versioned_object_by_id(self):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket = "verta-versioned-bucket"
        key = "data/census-train.csv"
        s3_url = "s3://{}/{}".format(bucket, key)

        # pick a version that's not the latest
        version_ids = [
            obj["VersionId"]
            for obj in s3.list_object_versions(Bucket=bucket)["Versions"]
            if not obj["IsLatest"] and obj["Key"] == key
        ]
        version_id = version_ids[0]

        s3_loc = verta.dataset._s3.S3Location(s3_url, version_id)
        dataset = verta.dataset.S3(s3_loc)

        assert len(dataset.list_components()) == 1
        assert dataset.list_components()[0].s3_version_id == version_id

    def test_versioned_folder(self):
        s3 = pytest.importorskip("boto3").client("s3")
        S3_PATH = verta.dataset.S3._S3_PATH

        bucket = "verta-versioned-bucket"
        folder = "data/"
        s3_url = "s3://{}/{}".format(bucket, folder)

        # collect latest versions of objects with folder as prefix
        version_ids = {
            S3_PATH.format(bucket, obj["Key"]): obj["VersionId"]
            for obj in s3.list_object_versions(Bucket=bucket, Prefix=folder)["Versions"]
            if obj["IsLatest"]
        }
        for path, version_id in version_ids.items():
            if version_id == "null":
                # S3 returns "null" in its API, but we handle that as empty string
                version_ids[path] = ""

        dataset = verta.dataset.S3(s3_url)

        for component in dataset.list_components():
            assert component.s3_version_id == version_ids[component.path]
            assert not component.path.endswith("/")
            assert component.size != 0

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        dataset_ver = verta.dataset.S3("s3://verta-starter")

        assert dataset_ver.__repr__()

    def test_list_paths(self):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket = "verta-starter"
        expected_paths = set(
            "s3://{}/{}".format(bucket, s3_obj["Key"])
            for s3_obj in s3.list_objects_v2(Bucket=bucket)["Contents"]
            if not s3_obj["Key"].endswith("/")  # folder, not object
        )

        dataset = verta.dataset.S3("s3://{}".format(bucket))
        assert set(dataset.list_paths()) == expected_paths

    def test_concat(self):
        dataset1 = verta.dataset.S3("s3://verta-starter/")
        dataset2 = verta.dataset.S3("s3://verta-versioned-bucket/")
        components = dataset1.list_components() + dataset2.list_components()
        components = list(sorted(components, key=lambda component: component.path))

        dataset = dataset1 + dataset2
        assert dataset.list_components() == components

        # commutative
        dataset = dataset2 + dataset1
        assert dataset.list_components() == components

        # assignment
        dataset1 += dataset2
        assert dataset1.list_components() == components

    def test_concat_intersect_error(self):
        dataset1 = verta.dataset.S3("s3://verta-starter/")
        dataset2 = verta.dataset.S3("s3://verta-starter/census-test.csv")

        with pytest.raises(ValueError):
            dataset1 + dataset2  # pylint: disable=pointless-statement

        # commutative
        with pytest.raises(ValueError):
            dataset2 + dataset1  # pylint: disable=pointless-statement

        # assignment
        with pytest.raises(ValueError):
            dataset1 += dataset2

    def test_concat_type_mismatch_error(self):
        dataset1 = verta.dataset.S3("s3://verta-starter/")
        dataset2 = verta.dataset.Path("modelapi_hypothesis/")

        with pytest.raises(TypeError):
            dataset1 + dataset2  # pylint: disable=pointless-statement

    def test_add(self):
        path1 = "s3://verta-starter/census-train.csv"
        path2 = "s3://verta-starter/census-test.csv"

        dataset = verta.dataset.S3(path1)
        dataset.add(path2)

        # as if we had added two separate blobs together
        dataset1 = verta.dataset.S3(path1)
        dataset2 = verta.dataset.S3(path2)
        components = dataset1.list_components() + dataset2.list_components()
        components = list(sorted(components, key=lambda component: component.path))

        assert dataset.list_components() == components

    def test_add_intersect_error(self):
        dataset = verta.dataset.S3("s3://verta-starter/")

        with pytest.raises(ValueError):
            dataset.add("s3://verta-starter/census-test.csv")


class TestPath:
    def test_dirpath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/")
        assert len(dataset.list_components()) > 1

        for component in dataset.list_components():
            assert component.path != ""
            assert component.size != 0
            assert component.last_modified != 0
            assert component.md5 != ""

    def test_filepath(self):
        dataset = verta.dataset.Path("modelapi_hypothesis/api_generator.py")

        assert len(dataset.list_components()) == 1

        component = dataset.list_components()[0]
        assert component.path != ""
        assert component.size != 0
        assert component.last_modified != 0
        assert component.md5 != ""

    def test_multiple_filepaths(self):
        dataset = verta.dataset.Path(
            [
                "modelapi_hypothesis/api_generator.py",
                "modelapi_hypothesis/test_modelapi.py",
            ]
        )
        assert len(dataset.list_components()) == 2

        for component in dataset.list_components():
            assert component.path != ""
            assert component.size != 0
            assert component.last_modified != 0
            assert component.md5 != ""

    def test_no_duplicates(self):
        multiple_dataset = verta.dataset.Path(
            [
                "modelapi_hypothesis/",
                "modelapi_hypothesis/api_generator.py",
            ]
        )
        dir_dataset = verta.dataset.Path("modelapi_hypothesis/")
        assert len(multiple_dataset.list_components()) == len(
            dir_dataset.list_components()
        )

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        dataset = verta.dataset.Path("modelapi_hypothesis/")

        assert dataset.__repr__()

    def test_list_paths(self):
        data_dir = "modelapi_hypothesis/"

        expected_paths = set(_file_utils.walk_files(data_dir))

        dataset = verta.dataset.Path(data_dir)
        assert set(dataset.list_paths()) == expected_paths

    @pytest.mark.parametrize(
        "paths, base_path",
        [
            # single path
            ([["../tests/modelapi_hypothesis"], ".."]),
            ([["../tests/modelapi_hypothesis"], "../tests"]),
            ([["../tests/modelapi_hypothesis"], "../tests/modelapi_hypothesis"]),
            ([["../tests/modelapi_hypothesis/"], "../tests/modelapi_hypothesis"]),
            # multiple paths
            ([["../tests/modelapi_hypothesis", "../setup.py"], ".."]),
            ([["../setup.py", "../tests/modelapi_hypothesis"], ".."]),
            ([["../tests/modelapi_hypothesis", "../tests/conftest.py"], "../tests"]),
        ],
    )
    def test_base_path(self, paths, base_path):
        filepaths = _file_utils.flatten_file_trees(paths)
        expected_paths = set(os.path.relpath(path, base_path) for path in filepaths)

        dataset = verta.dataset.Path(paths, base_path)
        assert set(dataset.list_paths()) == expected_paths

    @pytest.mark.parametrize(
        "paths, base_path",
        [
            # single path
            ([["../tests/modelapi_hypothesis"], "foo"]),
            ([["../tests/modelapi_hypothesis"], "../foo"]),
            ([["../tests/modelapi_hypothesis"], "../tests/modelapi_"]),
            # multiple unrelated paths
            ([["../tests/modelapi_hypothesis", "conftest.py"], ".."]),
            ([["conftest.py", "../tests/modelapi_hypothesis"], ".."]),
            ([["modelapi_hypothesis", "versioning"], "modelapi_hypothesis"]),
        ],
    )
    def test_invalid_base_path_error(self, paths, base_path):
        with pytest.raises(ValueError):
            verta.dataset.Path(paths, base_path)

    def test_concat(self):
        dataset1 = verta.dataset.Path("modelapi_hypothesis/")
        dataset2 = verta.dataset.Path("versioning/")
        components = dataset1.list_components() + dataset2.list_components()
        components = list(sorted(components, key=lambda component: component.path))

        dataset = dataset1 + dataset2
        assert dataset.list_components() == components

        # commutative
        dataset = dataset2 + dataset1
        assert dataset.list_components() == components

        # assignment
        dataset1 += dataset2
        assert dataset1.list_components() == components

    def test_concat_intersect_error(self):
        dataset1 = verta.dataset.Path("versioning/")
        dataset2 = verta.dataset.Path("versioning/test_dataset.py")

        with pytest.raises(ValueError):
            dataset1 + dataset2  # pylint: disable=pointless-statement

        # commutative
        with pytest.raises(ValueError):
            dataset2 + dataset1  # pylint: disable=pointless-statement

        # assignment
        with pytest.raises(ValueError):
            dataset1 += dataset2

    def test_concat_base_path(self):
        dataset1 = verta.dataset.Path(
            "modelapi_hypothesis/",
            base_path="modelapi_hypothesis/",
        )
        dataset2 = verta.dataset.Path(
            "versioning/",
            base_path="versioning/",
        )
        components = dataset1.list_components() + dataset2.list_components()
        components = list(sorted(components, key=lambda component: component.path))

        dataset = dataset1 + dataset2
        assert dataset.list_components() == components

    def test_concat_base_path_intersect_error(self):
        dataset1 = verta.dataset.Path(
            "./__init__.py",
            base_path=".",
        )
        dataset2 = verta.dataset.Path(
            "versioning/__init__.py",
            base_path="versioning",
        )

        with pytest.raises(ValueError):
            dataset1 + dataset2  # pylint: disable=pointless-statement

    def test_add(self):
        path1 = "versioning/test_code.py"
        path2 = "versioning/test_dataset.py"

        dataset = verta.dataset.Path(path1)
        dataset.add(path2)

        # as if we had added two separate blobs together
        dataset1 = verta.dataset.Path(path1)
        dataset2 = verta.dataset.Path(path2)
        components = dataset1.list_components() + dataset2.list_components()
        components = list(sorted(components, key=lambda component: component.path))

        assert dataset.list_components() == components

    def test_add_intersect_error(self):
        dataset = verta.dataset.Path("versioning/")

        with pytest.raises(ValueError):
            dataset.add("versioning/test_dataset.py")

    def test_file_scheme(self):
        filepaths = list(map(os.path.abspath, os.listdir(".")))
        prefixes = itertools.cycle({"file://", "file:", ""})
        prefixed_filepaths = (
            prefix + filepath for prefix, filepath in zip(prefixes, filepaths)
        )

        dataset1 = verta.dataset.Path(filepaths)
        dataset2 = verta.dataset.Path(prefixed_filepaths)

        assert set(dataset1.list_paths()) == set(dataset2.list_paths())

    def test_with_spark(self):
        filenames = list(map(os.path.abspath, ["versioning/"]))

        SparkContext = pytest.importorskip("pyspark").SparkContext
        sc = SparkContext("local")

        dataset1 = verta.dataset.Path.with_spark(sc, filenames)
        dataset2 = verta.dataset.Path(filenames)

        assert set(dataset1.list_paths()) == set(
            filterfalse(dataset2._is_hidden_to_spark, dataset2.list_paths())
        )


@pytest.mark.usefixtures("with_boto3", "in_tempdir")
class TestS3ManagedVersioning:
    def test_mngd_ver_file(self, dataset):
        s3 = pytest.importorskip("boto3").client("s3")

        filename = "tiny1.bin"
        bucket = "verta-versioned-bucket"
        key = "tiny-files/{}".format(filename)
        s3_key = "s3://{}/{}".format(bucket, key)
        blob_path = "data"

        # get file contents directly from S3 for reference
        s3.download_file(bucket, key, filename)
        with open(filename, "rb") as f:
            FILE_CONTENTS = f.read()
        os.remove(filename)

        # log & get dataset blob
        dataset_blob = verta.dataset.S3(s3_key, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        # download to implicit path
        filepath = dataset_blob.download(s3_key)
        assert os.path.isfile(filepath)
        assert filepath == os.path.abspath(filename)
        with open(filepath, "rb") as f:
            assert f.read() == FILE_CONTENTS

        # download to implicit path without collision
        filepath2 = dataset_blob.download(s3_key)
        assert os.path.isfile(filepath2)
        assert filepath2 != filepath
        with open(filepath2, "rb") as f:
            assert f.read() == FILE_CONTENTS

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(filepath)
        filepath3 = dataset_blob.download(s3_key, filepath)
        assert filepath3 == filepath
        with open(filepath3, "rb") as f:
            assert f.read() == FILE_CONTENTS
        assert os.path.getmtime(filepath) > last_updated

    def test_mngd_ver_folder(self, dataset):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket = "verta-versioned-bucket"
        dirname = "tiny-files/"
        s3_folder = "s3://{}/{}".format(bucket, dirname)
        blob_path = "data"

        # get files' contents directly from S3 for reference
        reference_dir = "reference/"
        for s3_obj in s3.list_objects_v2(Bucket=bucket, Prefix=dirname)["Contents"]:
            key = s3_obj["Key"]
            filepath = os.path.join(reference_dir, key)
            pathlib.Path(filepath).parent.mkdir(
                parents=True, exist_ok=True
            )  # create parent dirs

            s3.download_file(bucket, key, filepath)

        # Since we're retrieving files with the S3 prefix `dirname`, the downloaded filetree won't
        # start with `dirname`, so we have to go deeper for `reference_dir` to account for that.
        reference_dir = os.path.join(reference_dir, dirname)

        # log & get dataset blob
        dataset_blob = verta.dataset.S3(s3_folder, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        # download to implicit path
        dirpath = dataset_blob.download(s3_folder)
        assert os.path.isdir(dirpath)
        assert dirpath == os.path.abspath(dirname)
        assert_dirs_match(dirpath, reference_dir)

        # download to implicit path without collision
        dirpath2 = dataset_blob.download(s3_folder)
        assert os.path.isdir(dirpath2)
        assert dirpath2 != dirpath
        assert_dirs_match(dirpath2, reference_dir)

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(dirpath)
        dirpath3 = dataset_blob.download(s3_folder, dirpath)
        assert dirpath3 == dirpath
        assert_dirs_match(dirpath3, reference_dir)
        assert os.path.getmtime(dirpath) > last_updated

    def test_not_to_s3_dir(self, dataset):
        """If the user specifies "s3://", things shouldn't go into an "s3:" dir."""
        bucket = "verta-versioned-bucket"
        dirname = "tiny-files/"
        s3_folder = "s3://{}/{}".format(bucket, dirname)
        blob_path = "data"

        # log & get dataset blob
        dataset_blob = verta.dataset.S3(s3_folder, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        dirpath = dataset_blob.download("s3://")
        assert "s3:" not in pathlib.Path(dirpath).parts

    def test_download_all(self, dataset):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket = "verta-versioned-bucket"
        dirname = "tiny-files/"
        s3_folder = "s3://{}/{}".format(bucket, dirname)

        # get files' contents directly from S3 for reference
        reference_dir = "reference/"
        for s3_obj in s3.list_objects_v2(Bucket=bucket, Prefix=dirname)["Contents"]:
            key = s3_obj["Key"]
            filepath = os.path.join(reference_dir, bucket, key)
            pathlib.Path(filepath).parent.mkdir(
                parents=True, exist_ok=True
            )  # create parent dirs

            s3.download_file(bucket, key, filepath)

        # log & get dataset blob
        dataset_blob = verta.dataset.S3(s3_folder, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        dirpath = dataset_blob.download()
        assert dirpath == os.path.abspath(_dataset.DEFAULT_DOWNLOAD_DIR)

        assert os.path.isdir(dirpath)
        assert_dirs_match(dirpath, reference_dir)

    def test_concat(self, dataset):
        s3 = pytest.importorskip("boto3").client("s3")

        bucket1 = "verta-starter"
        key1 = "models/model.pkl"
        bucket2 = "verta-versioned-bucket"
        key2 = "tiny-files/tiny2.bin"

        # create dir for reference files
        reference_dir = "reference"
        filepath1 = os.path.join(reference_dir, bucket1, key1)
        pathlib.Path(filepath1).parent.mkdir(parents=True, exist_ok=True)
        filepath2 = os.path.join(reference_dir, bucket2, key2)
        pathlib.Path(filepath2).parent.mkdir(parents=True, exist_ok=True)

        # download files directly from S3 for reference
        s3.download_file(bucket1, key1, filepath1)
        s3.download_file(bucket2, key2, filepath2)

        # create and concatenate datasets
        dataset1 = verta.dataset.S3(
            "s3://{}/{}".format(bucket1, key1),
            enable_mdb_versioning=True,
        )
        dataset2 = verta.dataset.S3(
            "s3://{}/{}".format(bucket2, key2),
            enable_mdb_versioning=True,
        )
        dataset_blob = dataset1 + dataset2
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        dirpath = dataset_blob.download()
        assert_dirs_match(dirpath, reference_dir)

    def test_concat_arg_mismatch_error(self):
        dataset1 = verta.dataset.S3(
            "s3://verta-starter/",
            enable_mdb_versioning=True,
        )
        dataset2 = verta.dataset.S3(
            "s3://verta-versioned-bucket/",
            enable_mdb_versioning=False,
        )

        with pytest.raises(ValueError):
            dataset1 + dataset2  # pylint: disable=pointless-statement


@pytest.mark.usefixtures("in_tempdir")
class TestPathManagedVersioning:
    def test_mngd_ver_file(self, dataset):
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, "wb") as f:
            f.write(FILE_CONTENTS)
        blob_path = "data"

        dataset_blob = verta.dataset.Path(filename, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()
        os.remove(filename)  # delete for first download test

        # download to implicit path
        filepath = dataset_blob.download(filename)
        assert os.path.isfile(filepath)
        assert filepath == os.path.abspath(filename)
        with open(filepath, "rb") as f:
            assert f.read() == FILE_CONTENTS

        # download to implicit path without collision
        filepath2 = dataset_blob.download(filename)
        assert os.path.isfile(filepath2)
        assert filepath2 != filepath
        with open(filepath2, "rb") as f:
            assert f.read() == FILE_CONTENTS

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(filepath)
        filepath3 = dataset_blob.download(filename, filepath)
        assert filepath3 == filepath
        with open(filepath3, "rb") as f:
            assert f.read() == FILE_CONTENTS
        assert os.path.getmtime(filepath) > last_updated

    def test_mngd_ver_folder(self, dataset):
        reference_dir = "reference/"
        dirname = "tiny-files/"
        os.mkdir(dirname)
        for filename in ["tiny{}.bin".format(i) for i in range(3)]:
            with open(os.path.join(dirname, filename), "wb") as f:
                f.write(os.urandom(2**16))

        blob_path = "data"
        dataset_blob = verta.dataset.Path(dirname, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()
        shutil.move(dirname, reference_dir)  # move sources to avoid collision

        # download to implicit path
        dirpath = dataset_blob.download(dirname)
        assert os.path.isdir(dirpath)
        assert dirpath == os.path.abspath(dirname)
        assert_dirs_match(dirpath, reference_dir)

        # download to implicit path without collision
        dirpath2 = dataset_blob.download(dirname)
        assert os.path.isdir(dirpath2)
        assert dirpath2 != dirpath
        assert_dirs_match(dirpath2, reference_dir)

        # download to explicit path with overwrite
        last_updated = os.path.getmtime(dirpath)
        dirpath3 = dataset_blob.download(dirname, dirpath)
        assert dirpath3 == dirpath
        assert_dirs_match(dirpath3, reference_dir)
        assert os.path.getmtime(dirpath) > last_updated

    def test_mngd_ver_to_parent_dir(self, dataset):
        """Download to parent directory works as expected."""
        child_dirname = "child"
        os.mkdir(child_dirname)
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)

        with utils.chdir(child_dirname):
            with open(filename, "wb") as f:
                f.write(FILE_CONTENTS)
            blob_path = "data"

            dataset_blob = verta.dataset.Path(filename, enable_mdb_versioning=True)
            dataset_blob = dataset.create_version(dataset_blob).get_content()

            # download to parent dir
            download_to_path = os.path.join("..", filename)
            filepath = dataset_blob.download(filename, download_to_path)
            assert os.path.isfile(filepath)
            assert filepath == os.path.abspath(download_to_path)
            with open(filepath, "rb") as f:
                assert f.read() == FILE_CONTENTS

    def test_mngd_ver_to_sibling_dir(self, dataset):
        """Download to sibling directory works as expected."""
        child_dirname = "child"
        os.mkdir(child_dirname)
        sibling_dirname = "sibling"
        os.mkdir(sibling_dirname)
        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)

        with utils.chdir(child_dirname):
            with open(filename, "wb") as f:
                f.write(FILE_CONTENTS)
            blob_path = "data"

            dataset_blob = verta.dataset.Path(filename, enable_mdb_versioning=True)
            dataset_blob = dataset.create_version(dataset_blob).get_content()

            # download to sibling dir
            download_to_path = os.path.join("..", sibling_dirname, filename)
            filepath = dataset_blob.download(filename, download_to_path)
            assert os.path.isfile(filepath)
            assert filepath == os.path.abspath(download_to_path)
            with open(filepath, "rb") as f:
                assert f.read() == FILE_CONTENTS

    def test_download_all(self, dataset):
        reference_dir = "tiny-files/"
        os.mkdir(reference_dir)
        for filename in ["tiny{}.bin".format(i) for i in range(3)]:
            with open(os.path.join(reference_dir, filename), "wb") as f:
                f.write(os.urandom(2**16))

        # log & get dataset blob
        blob_path = "data"
        dataset_blob = verta.dataset.Path(reference_dir, enable_mdb_versioning=True)
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        dirpath = dataset_blob.download()
        assert dirpath == os.path.abspath(_dataset.DEFAULT_DOWNLOAD_DIR)

        # uploaded filetree was recreated within `DEFAULT_DOWNLOAD_DIR`
        destination_dir = os.path.join(_dataset.DEFAULT_DOWNLOAD_DIR, reference_dir)
        assert os.path.isdir(destination_dir)
        assert_dirs_match(destination_dir, reference_dir)

    def test_base_path(self, dataset):
        reference_dir = "tiny-files/"
        os.mkdir(reference_dir)
        # three .file files in tiny-files/
        for filename in ["tiny{}.file".format(i) for i in range(3)]:
            with open(os.path.join(reference_dir, filename), "wb") as f:
                f.write(os.urandom(2**16))

        sub_dir = "bin/"
        os.mkdir(os.path.join(reference_dir, sub_dir))
        # three .bin files in tiny-files/bin/
        for filename in ["tiny{}.bin".format(i) for i in range(3)]:
            with open(os.path.join(reference_dir, sub_dir, filename), "wb") as f:
                f.write(os.urandom(2**16))

        # log & get dataset blob
        blob_path = "data"
        dataset_blob = verta.dataset.Path(
            reference_dir,
            base_path=reference_dir,
            enable_mdb_versioning=True,
        )
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        # `reference_dir` was dropped as base path, so KeyError
        with pytest.raises(KeyError):
            dataset_blob.download(reference_dir)

        dirpath = dataset_blob.download()
        assert os.path.abspath(dirpath) != os.path.abspath(reference_dir)
        assert_dirs_match(dirpath, reference_dir)

    def test_concat(self, dataset):
        reference_dir = "tiny-files/"
        os.mkdir(reference_dir)
        # two .file files in tiny-files/
        for filename in ["tiny{}.file".format(i) for i in range(2)]:
            with open(os.path.join(reference_dir, filename), "wb") as f:
                f.write(os.urandom(2**16))

        # create and concatenate datasets
        dataset1 = verta.dataset.Path(
            "tiny-files/tiny0.file",
            enable_mdb_versioning=True,
        )
        dataset2 = verta.dataset.Path(
            "tiny-files/tiny1.file",
            enable_mdb_versioning=True,
        )
        dataset_blob = dataset1 + dataset2
        dataset_blob = dataset.create_version(dataset_blob).get_content()

        dirpath = dataset_blob.download()
        dirpath = os.path.join(
            dirpath, reference_dir
        )  # "tiny-files/" nested in new dir
        assert_dirs_match(dirpath, reference_dir)

    def test_concat_arg_mismatch_error(self):
        reference_dir = "tiny-files/"
        os.mkdir(reference_dir)
        # two .file files in tiny-files/
        for filename in ["tiny{}.file".format(i) for i in range(2)]:
            with open(os.path.join(reference_dir, filename), "wb") as f:
                f.write(os.urandom(2**16))

        dataset1 = verta.dataset.Path(
            "tiny-files/tiny0.file",
            enable_mdb_versioning=True,
        )
        dataset2 = verta.dataset.Path(
            "tiny-files/tiny1.file",
            enable_mdb_versioning=False,
        )

        with pytest.raises(ValueError):
            dataset1 + dataset2  # pylint: disable=pointless-statement
