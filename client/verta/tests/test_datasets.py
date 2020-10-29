import pytest

import six

import os
import time
import shutil
import requests

from . import utils

import verta
import verta.dataset
from verta._internal_utils import _utils
from verta._dataset import Dataset, DatasetVersion, S3DatasetVersionInfo, FilesystemDatasetVersionInfo
from verta._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from verta._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService


DEFAULT_S3_TEST_BUCKET = "bucket"
DEFAULT_S3_TEST_OBJECT = "object"
DEFAULT_GOOGLE_APPLICATION_CREDENTIALS = "credentials.json"

# for `tags` typecheck tests
TAG = "my-tag"


@pytest.fixture(scope='session')
def s3_bucket():
    return os.environ.get("VERTA_S3_TEST_BUCKET", DEFAULT_S3_TEST_BUCKET)


@pytest.fixture(scope='session')
def s3_object():
    return os.environ.get("VERTA_S3_TEST_OBJECT", DEFAULT_S3_TEST_OBJECT)


@pytest.fixture(scope='session')
def bq_query():
    return (
        "SELECT id, `by`, score, time, time_ts, title, url, text, deleted, dead, descendants, author"
        " FROM `bigquery-public-data.hacker_news.stories`"
        " LIMIT 1000"
    )


@pytest.fixture(scope='session')
def bq_location():
    return "US"


class TestBaseDatasets:
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        assert dataset.id

    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        assert dataset.id

        same_dataset = Dataset(client._conn, client._conf,
                               _dataset_id=dataset.id)
        assert dataset.id == same_dataset.id


class TestBaseDatasetVersions:
    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.PathDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        assert version.id

    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.PathDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        assert version.id

        same_version = DatasetVersion(client._conn, client._conf,
                                      _dataset_version_id=version.id)
        assert version.id == same_version.id

    @pytest.mark.parametrize("tags", [TAG, [TAG]])
    @pytest.mark.skip(reason="api no longer  supported by backend")
    def test_tags_is_list_of_str(self, client, created_datasets, tags):
        dataset = client.set_dataset(tags=tags)
        created_datasets.append(dataset)
        version = dataset.create_version("conftest.py", tags=tags)

        endpoint = "{}://{}/api/v1/modeldb/dataset-version/getDatasetVersionTags".format(
            client._conn.scheme,
            client._conn.socket,
        )
        response = verta._internal_utils._utils.make_request("GET", endpoint, client._conn, params={'id': version.id})
        verta._internal_utils._utils.raise_for_http_error(response)
        assert response.json().get('tags', []) == [TAG]


# TODO: not implemented
class TestRawDatasets:
    pass


# TODO: not implemented
class TestRawDatasetVersions:
    pass


class TestPathDatasets:
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        assert dataset.id

    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)
        assert dataset.id

        same_dataset = Dataset(client._conn, client._conf,
                               _dataset_id=dataset.id)
        assert dataset.id == same_dataset.id


class TestClientDatasetFunctions:
    def test_creation_from_scratch_client_api(self, client, created_datasets):
        dataset = client.set_dataset(type="s3")
        created_datasets.append(dataset)
        assert dataset.id
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client.set_dataset(name=dataset.name, desc="new description")

    def test_creation_by_id_client_api(self, client, created_datasets):
        dataset = client.set_dataset(type="s3")
        created_datasets.append(dataset)
        assert dataset.id

        same_dataset = client.set_dataset(id=dataset.id)
        assert dataset.id == same_dataset.id
        assert dataset.name == same_dataset.name

    def test_get_dataset_client_api(self, client, created_datasets):
        dataset = client.set_dataset(type="s3")
        created_datasets.append(dataset)
        assert dataset.id

        same_dataset = client.get_dataset(id=dataset.id)
        assert dataset.id == same_dataset.id
        assert dataset.name == same_dataset.name

        same_dataset = client.get_dataset(name=dataset.name)
        assert dataset.id == same_dataset.id
        assert dataset.name == same_dataset.name

    def test_find_datasets_by_fuzzy_name(self, client, created_datasets):
        now = str(_utils.now())
        created_datasets.append(client.set_dataset(now+" appl"))
        created_datasets.append(client.set_dataset(now+" Appl"))
        created_datasets.append(client.set_dataset(now+" Apple"))

        datasets = client.find_datasets(name=now+" Appl")
        assert len(datasets) == 3

    @pytest.mark.skip("See #1285")
    def test_find_datasets_client_api(self, client, created_datasets):
        tags = ["test1a-{}".format(_utils.now()), "test1b-{}".format(_utils.now())]
        dataset1 = client.set_dataset(type="big query", tags=tags)
        created_datasets.append(dataset1)
        assert dataset1.id

        single_tag = ["test2-{}".format(_utils.now())]
        dataset2 = client.set_dataset(type="s3", tags=single_tag)
        created_datasets.append(dataset2)
        assert dataset2.id

        # TODO: update once RAW is supported
        # dataset3 = client.set_dataset(type="raw")
        # created_datasets.append(dataset3)
        # assert dataset3._dataset_type == _DatasetService.DatasetTypeEnum.RAW
        # assert dataset3.id

        # datasets = client.find_datasets()
        # assert len(datasets) == 3
        # assert datasets[0].id == dataset1.id
        # assert datasets[1].id == dataset2.id
        # assert datasets[2].id == dataset3.id

        datasets = client.find_datasets(tags=tags)
        assert len(datasets) == 1
        assert datasets[0].id == dataset1.id

        # str arg automatically wrapped into list by client
        datasets = client.find_datasets(tags=single_tag[0])
        assert len(datasets) == 1
        assert datasets[0].id == dataset2.id

        datasets = client.find_datasets(name=dataset1.name)
        assert len(datasets) == 1
        assert datasets[0].id == dataset1.id

        datasets = client.find_datasets(dataset_ids=[dataset1.id, dataset2.id], name=dataset1.name)
        assert len(datasets) == 1
        assert datasets[0].id == dataset1.id

        # test sorting ascending
        datasets = client.find_datasets(
            dataset_ids=[dataset1.id, dataset2.id],
            sort_key="time_created", ascending=True,
        )
        assert [dataset.id for dataset in datasets] == [dataset1.id, dataset2.id]
        # and descending
        datasets = client.find_datasets(
            dataset_ids=[dataset1.id, dataset2.id],
            sort_key="time_created", ascending=False,
        )
        assert [dataset.id for dataset in datasets] == [dataset2.id, dataset1.id]


class TestClientDatasetVersionFunctions:
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        version = dataset.create_version(__file__)
        assert version.id

    def test_creation_by_id(self, client, created_datasets):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        version = dataset.create_version(__file__)
        assert version.id

        same_version = client.get_dataset_version(id=version.id)
        assert version.id == same_version.id

    def test_get_versions(self, client, created_datasets):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        version1 = dataset.create_version(path=__file__)
        assert version1.id

        version2 = dataset.create_version(path=pytest.__file__)
        assert version2.id

        versions = dataset.get_all_versions()
        assert len(versions) == 2

        dataset_version1 = client.get_dataset_version(id=version1.id)
        assert dataset_version1.id == version1.id

        version = dataset.get_latest_version(ascending=True)
        assert version.id == version1.id

    def test_get_latest_printing(self, client, created_datasets, capsys):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        version = dataset.create_version(path=__file__)
        dataset.get_latest_version(ascending=True)

        captured = capsys.readouterr()
        assert "got existing dataset version: {}".format(version.id) in captured.out

    def test_dataset_version_info(self, client, created_datasets):
        botocore = pytest.importorskip("botocore")
        try:
            s3_dataset = client.set_dataset(type="s3")
            created_datasets.append(s3_dataset)
            s3_version = s3_dataset.create_version("verta-starter", key="census-train.csv")
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")

        local_dataset = client.set_dataset(type="local")
        created_datasets.append(local_dataset)
        local_version = local_dataset.create_version(__file__)

        for retrieved, version in [(s3_dataset.get_latest_version(), s3_version),
                                   (local_dataset.get_latest_version(), local_version)]:
            assert retrieved.dataset_version_info is not None
            assert retrieved.dataset_version_info == version.dataset_version_info

    @pytest.mark.skip(reason="functionality removed")
    def test_reincarnation(self, client, created_datasets):
        """Consecutive identical versions are assigned the same ID."""
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        version1 = dataset.create_version(path=__file__)
        version2 = dataset.create_version(path=__file__)
        assert version1.id == version2.id

        versions = dataset.get_all_versions()
        assert len(versions) == 1

        version = dataset.get_latest_version(ascending=True)
        assert version.id == version1.id


class TestBasePath:
    """Test restored base path accessibility."""
    @staticmethod
    def assert_base_path(dataset_version, base_path):
        # base path accessible on components
        for component in dataset_version.list_components():
            assert component.base_path == base_path

        # base path accessible on dataset version
        assert dataset_version.base_path == base_path

        # base path accessible via proto for backwards-compatibility
        assert dataset_version.dataset_version.path_dataset_version_info.base_path == base_path
        assert dataset_version.dataset_version_info.base_path == base_path

    def test_s3_bucket(self, client, created_datasets):
        bucket_name = "verta-starter"

        botocore = pytest.importorskip("botocore")
        try:
            dataset = client.set_dataset(type="s3")
            created_datasets.append(dataset)
            version = dataset.create_version(bucket_name)
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")

        retrieved = dataset.get_latest_version()
        assert version.id == retrieved.id  # of course, but just to be sure

        self.assert_base_path(version, bucket_name)
        self.assert_base_path(retrieved, bucket_name)

    def test_s3_obj(self, client, created_datasets):
        bucket_name = "verta-starter"
        obj_name = "census-train.csv"

        botocore = pytest.importorskip("botocore")
        try:
            dataset = client.set_dataset(type="s3")
            created_datasets.append(dataset)
            version = dataset.create_version(bucket_name, obj_name)
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")

        retrieved = dataset.get_latest_version()
        assert version.id == retrieved.id  # of course, but just to be sure

        base_path = '/'.join([bucket_name, obj_name])
        self.assert_base_path(version, base_path)
        self.assert_base_path(retrieved, base_path)

    def test_local_dir(self, client, created_datasets):
        dirpath = "."

        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)
        version = dataset.create_version(dirpath)

        retrieved = dataset.get_latest_version()
        assert version.id == retrieved.id  # of course, but just to be sure

        base_path = os.path.abspath(dirpath)
        self.assert_base_path(version, base_path)
        self.assert_base_path(retrieved, base_path)

    def test_local_file(self, client, created_datasets):
        filepath = "conftest.py"

        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)
        version = dataset.create_version(filepath)

        retrieved = dataset.get_latest_version()
        assert version.id == retrieved.id  # of course, but just to be sure

        base_path = os.path.abspath(filepath)
        self.assert_base_path(version, base_path)
        self.assert_base_path(retrieved, base_path)


class TestPathBasedDatasetVersions:
    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)

        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.PathDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        assert version.id

    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        created_datasets.append(dataset)

        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.PathDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.PATH)
        assert version.id

        same_version = DatasetVersion(client._conn, client._conf,
                                      _dataset_version_id=version.id)
        assert version.id == same_version.id


class TestQueryDatasets:
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.DatasetType.QUERY)
        created_datasets.append(dataset)
        assert dataset.id

    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.DatasetType.QUERY)
        created_datasets.append(dataset)
        assert dataset.id

        same_dataset = Dataset(client._conn, client._conf,
                               _dataset_id=dataset.id)
        assert dataset.id == same_dataset.id


class TestQueryDatasetVersions:
    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_from_scratch(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.DatasetType.QUERY)
        created_datasets.append(dataset)

        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.QueryDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.QUERY)
        assert version.id

    @pytest.mark.skip(reason="direct instantiation of info-less DatasetVersion not supported by backend")
    def test_creation_by_id(self, client, created_datasets):
        dataset = Dataset(client._conn, client._conf,
                          dataset_type=_DatasetService.DatasetTypeEnum.QUERY)
        created_datasets.append(dataset)

        version = DatasetVersion(client._conn, client._conf,
                                 dataset_id=dataset.id,
                                 dataset_version_info=_DatasetVersionService.QueryDatasetVersionInfo(),
                                 dataset_type=_DatasetService.DatasetTypeEnum.QUERY)
        assert version.id

        same_version = DatasetVersion(client._conn, client._conf,
                                      _dataset_version_id=version.id)
        assert version.id == same_version.id


class TestFileSystemDatasetVersionInfo:
    def test_single_file(self):
        dir_name, file_names = self.create_dir_with_files(num_files=1)
        fsdvi = FilesystemDatasetVersionInfo(dir_name + "/" + file_names[0])
        assert len(fsdvi.dataset_part_infos) == 1
        assert fsdvi.size == 7
        shutil.rmtree(dir_name)

    def test_dir(self):
        dir_name, _ = self.create_dir_with_files(num_files=10)
        fsdvi = FilesystemDatasetVersionInfo(dir_name)
        assert len(fsdvi.dataset_part_infos) == 10
        assert fsdvi.size == 70
        shutil.rmtree(dir_name)

    def create_dir_with_files(self, num_files=10):
        dir_name = 'FSD:' + str(time.time())
        file_names = []
        os.mkdir(dir_name)
        for num_file in range(num_files):
            file_name = str(num_file) + ".txt"
            f = open(dir_name + "/" + file_name, 'w')
            f.write('123456\n')
            file_names.append(file_name)
        return dir_name, file_names


class TestS3DatasetVersionInfo:
    def test_single_object(self, s3_bucket, s3_object):
        botocore = pytest.importorskip("botocore")

        try:
            s3dvi = S3DatasetVersionInfo(s3_bucket, s3_object)
            assert len(s3dvi.dataset_part_infos) == 1
            assert s3dvi.size > 0
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")

    def test_bucket(self, s3_bucket):
        botocore = pytest.importorskip("botocore")

        try:
            s3dvi = S3DatasetVersionInfo(s3_bucket)
            assert len(s3dvi.dataset_part_infos) >= 1
            assert s3dvi.size > 0
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")


class TestS3ClientFunctions:
    def test_s3_dataset_creation(self, client, created_datasets):
        botocore = pytest.importorskip("botocore")

        try:
            dataset = client.set_dataset(type="s3")
            created_datasets.append(dataset)
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")

    def test_s3_dataset_version_creation(self, client, s3_bucket, created_datasets):
        botocore = pytest.importorskip("botocore")

        try:
            dataset = client.set_dataset(type="s3")
            created_datasets.append(dataset)
            dataset_version = dataset.create_version(s3_bucket)

            assert len(dataset_version.dataset_version_info.dataset_part_infos) >= 1
        except botocore.exceptions.ClientError:
            pytest.skip("insufficient AWS credentials")


class TestFilesystemClientFunctions:
    def test_filesystem_dataset_creation(self, client, created_datasets):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

    def test_filesystem_dataset_version_creation(self, client, created_datasets):
        dir_name, _ = self.create_dir_with_files(num_files=3)
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)
        dataset_version = dataset.create_version(dir_name)

        assert len(dataset_version.dataset_version_info.dataset_part_infos) == 3
        shutil.rmtree(dir_name)

    def create_dir_with_files(self, num_files=10):
        dir_name = 'FSD:' + str(time.time())
        file_names = []
        os.mkdir(dir_name)
        for num_file in range(num_files):
            file_name = str(num_file) + ".txt"
            f = open(dir_name + "/" + file_name, 'w')
            f.write('123456\n')
            file_names.append(file_name)
        return dir_name, file_names


@pytest.mark.skip("dropped support for query datasets, for the time being")
class TestBigQueryDatasetVersionInfo:
    def test_big_query_dataset(self, client, created_datasets):
        dataset = client.set_dataset(type="big query")
        created_datasets.append(dataset)

    def test_big_query_dataset_version_creation(self, client, bq_query, bq_location, created_datasets):
        google = pytest.importorskip("google")
        bigquery = pytest.importorskip("google.cloud.bigquery")

        try:
            query_job = google.cloud.bigquery.Client().query(
                bq_query,
                # Location must match that of the dataset(s) referenced in the query.
                location=bq_location,
            )
            dataset = client.set_dataset(type="big query")
            created_datasets.append(dataset)
            dataset_version = dataset.create_version(job_id=query_job.job_id, location=bq_location)

            assert dataset_version.dataset_version_info.query == bq_query
        except google.auth.exceptions.GoogleAuthError:
            pytest.skip("insufficient GCP credentials")


@pytest.mark.skip("dropped support for query datasets, for the time being")
class TestRDBMSDatasetVersionInfo:
    def test_rdbms_dataset(self, client, created_datasets):
        dataset = client.set_dataset(type="postgres")
        created_datasets.append(dataset)

    def test_rdbms_version_creation(self, client, created_datasets):
        dataset = client.set_dataset(type="postgres")
        created_datasets.append(dataset)
        dataset_version = dataset.create_version(query="SELECT * FROM ner-table",
                                                 db_connection_str="localhost:6543",
                                                 num_records=100)

        assert dataset_version.dataset_version_info.query == "SELECT * FROM ner-table"
        assert dataset_version.dataset_version_info.data_source_uri == "localhost:6543"
        assert dataset_version.dataset_version_info.num_records == 100

@pytest.mark.skip("Backend needs to be fixed to preserve `base_path`")
class TestLogDatasetVersion:
    def test_log_dataset_version(self, client, created_datasets, experiment_run):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        dataset_version = dataset.create_version(__file__)
        experiment_run.log_dataset_version('train', dataset_version)

        retrieved_dataset_version = experiment_run.get_dataset_version('train')
        path = retrieved_dataset_version.dataset_version.path_dataset_version_info.base_path
        assert path.endswith(__file__)

    @pytest.mark.not_oss
    def test_log_dataset_version_diff_workspaces(self, client, organization, created_datasets, experiment_run):
        dataset = client.set_dataset(type="local", workspace=organization.name)
        created_datasets.append(dataset)

        dataset_version = dataset.create_version(__file__)
        experiment_run.log_dataset_version('train', dataset_version)

        retrieved_dataset_version = experiment_run.get_dataset_version('train')
        assert retrieved_dataset_version.id == dataset_version.id

    def test_log_dataset_version_diff_workspaces_no_access_error(self, client_2, created_datasets, experiment_run):
        dataset = client_2.set_dataset(type="local")
        created_datasets.append(dataset)

        dataset_version = dataset.create_version(__file__)

        with pytest.raises(requests.HTTPError) as excinfo:
            experiment_run.log_dataset_version('train', dataset_version)

        excinfo_value = str(excinfo.value).strip()
        assert "403" in excinfo_value
        assert "Access Denied" in excinfo_value


    def test_overwrite(self, client, created_datasets, experiment_run, s3_bucket):
        dataset = client.set_dataset(type="local")
        created_datasets.append(dataset)

        dataset_version = dataset.create_version(__file__)
        experiment_run.log_dataset_version('train', dataset_version)

        new_dataset_version = dataset.create_version("conftest.py")
        experiment_run.log_dataset_version('train', new_dataset_version, overwrite=True)

        retrieved_dataset_version = experiment_run.get_dataset_version('train')
        path = retrieved_dataset_version.dataset_version.path_dataset_version_info.base_path
        assert path.endswith("conftest.py")
