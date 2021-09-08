# -*- coding: utf-8 -*-

import os
import tarfile

import pytest

import six

import requests

import yaml

import verta
from verta._internal_utils import (
    _artifact_utils,
    _histogram_utils,
    _utils,
)
from verta.environment import Python

pytestmark = pytest.mark.not_oss


class TestLogModelForDeployment:
    def test_model(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_model = experiment_run.get_model()

        assert model_for_deployment['model'].get_params() == retrieved_model.get_params()

    def test_model_api(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_model_api = verta.utils.ModelAPI.from_file(
            experiment_run.get_artifact(_artifact_utils.MODEL_API_KEY))

        assert all(item in six.viewitems(retrieved_model_api.to_dict())
                   for item in six.viewitems(model_for_deployment['model_api'].to_dict()))

    def test_reqs_on_disk(self, experiment_run, model_for_deployment, output_path):
        requirements_file = output_path.format("requirements.txt")
        with open(requirements_file, 'w') as f:
            f.write(model_for_deployment['requirements'].read())
        model_for_deployment['requirements'] = open(requirements_file, 'r')  # replace with on-disk file

        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_requirements = six.ensure_str(experiment_run.get_artifact("requirements.txt").read())

        with open(requirements_file, 'r') as f:
            assert set(f.read().split()) <= set(retrieved_requirements.split())


class TestHistogram:
    @staticmethod
    def assert_histograms_match_dataframe(histograms, df):
        """Common assertions for this test suite."""
        np = pytest.importorskip("numpy")

        # features match
        assert set(histograms['features'].keys()) == set(df.columns)
        # all rows counted
        assert histograms['total_count'] == len(df.index)

        for feature_name, histogram in histograms['features'].items():
            series = df[feature_name]
            histogram_type = histogram['type']
            histogram_data = histogram['histogram'][histogram_type]

            # all data points counted
            counts = histogram_data['count']
            assert sum(counts) == len(series)

            if histogram_type == "binary":
                num_false = sum(~series)
                num_true = sum(series)

                assert counts == [num_false, num_true]
            elif histogram_type == "discrete":
                buckets = histogram_data['bucket_values']

                # buckets in ascending order
                assert buckets == list(sorted(buckets))

                # data within buckets
                assert all(buckets[0] <= series)
                assert all(series <= buckets[-1])

                # appropriate leftmost and rightmost buckets
                assert buckets[0] == series.min()
                assert buckets[-1] == series.max()

                # all buckets have data
                # NOTE: this might not be behavior that we want in the future
                assert all(counts)

                # counts correct
                for value, count in zip(buckets, counts):
                    assert sum(series == value) == count
            elif histogram_type == "float":
                limits = histogram_data['bucket_limits']

                # limits in ascending order
                assert limits == list(sorted(limits))

                # data within limits
                assert all(limits[0] <= series)
                assert all(series <= limits[-1])

                # appropriate leftmost and rightmost limits
                assert np.isclose(limits[0], series.min())
                assert np.isclose(limits[-1], series.max())

                # buckets equal in width
                bucket_widths = np.diff(limits)
                assert np.allclose(bucket_widths, bucket_widths[0])

                # correct number of buckets
                assert len(limits) == 11

                # counts correct
                bin_windows = list(zip(limits[:-1], limits[1:]))
                for i, (l, r) in enumerate(bin_windows[:-1]):
                    assert sum((l <= series) & (series < r)) == counts[i]
                assert sum(limits[-2] <= series) == counts[-1]

    def test_binary(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="A"),
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="B"),
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "binary"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)

    def test_discrete(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.randint(6, 12, size=num_rows), name="A"),
                pd.Series(np.random.randint(-12, -6, size=num_rows), name="B"),
                pd.Series(np.random.randint(-3, 3, size=num_rows), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "discrete"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)

    def test_float(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.normal(loc=9, size=num_rows), name="A"),
                pd.Series(np.random.normal(scale=12, size=num_rows), name="B"),
                pd.Series(np.random.normal(loc=-3, scale=6, size=num_rows), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "float"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)


@pytest.mark.skip(reason="old deployment API is being phased out (VR-7935)")
class TestDeploy:
    def test_auto_path_auto_token_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy()

        assert 'url' in status
        assert 'token' in status

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_auto_path_given_token_deploy(self, experiment_run, model_for_deployment):
        token = "coconut"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(token=token)

        assert 'url' in status
        assert status['token'] == token

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_auto_path_no_token_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(no_token=True)

        assert 'url' in status
        assert status['token'] is None

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_auto_token_deploy(self, experiment_run, model_for_deployment):
        path = "banana"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(path=path)

        assert status['url'].endswith(path)
        assert 'token' in status

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_given_token_deploy(self, experiment_run, model_for_deployment):
        path, token = "banana", "coconut"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(path=path, token=token)

        assert status['url'].endswith(path)
        assert status['token'] == token

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_no_token_deploy(self, experiment_run, model_for_deployment):
        path = "banana"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(path=path, no_token=True)

        assert status['url'].endswith(path)
        assert status['token'] is None

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_wait_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        status = experiment_run.deploy(wait=True)

        assert 'url' in status
        assert 'token' in status
        assert status['status'] == "deployed"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_already_deployed_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        experiment_run.deploy()

        # should not raise error
        experiment_run.deploy()

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_model_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        # delete model
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(experiment_run._conn.scheme,
                                                              experiment_run._conn.socket),
            experiment_run._conn, json={'id': experiment_run.id, 'key': _artifact_utils.MODEL_KEY}
        )
        _utils.raise_for_http_error(response)

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert _artifact_utils.MODEL_KEY in str(excinfo.value)

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_api_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        # delete model API
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(experiment_run._conn.scheme,
                                                              experiment_run._conn.socket),
            experiment_run._conn, json={'id': experiment_run.id, 'key': _artifact_utils.MODEL_API_KEY}
        )
        _utils.raise_for_http_error(response)

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert _artifact_utils.MODEL_API_KEY in str(excinfo.value)

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_reqs_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert "requirements.txt" in str(excinfo.value)

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_deployment_failure_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python([]))

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy(wait=True)
        err_msg = str(excinfo.value).strip()
        assert err_msg.startswith("model deployment is failing;")
        assert "no error message available" not in err_msg

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )


@pytest.mark.skip(reason="old deployment API is being phased out (VR-7935)")
class TestUndeploy:
    def test_undeploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        experiment_run.deploy(wait=True)

        status = experiment_run.undeploy(wait=True)

        assert status['status'] == "not deployed"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_already_undeployed_undeploy(self, experiment_run):
        # should not raise error
        experiment_run.undeploy()


@pytest.mark.skip(reason="old deployment API is being phased out (VR-7935)")
class TestGetDeployedModel:
    def test_get(self, experiment_run, model_for_deployment):
        model = model_for_deployment['model'].fit(
            model_for_deployment['train_features'],
            model_for_deployment['train_targets'],
        )

        experiment_run.log_model(model, custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        experiment_run.deploy(wait=True)

        deployed_model = experiment_run.get_deployed_model()
        x = model_for_deployment['train_features'].iloc[1].values
        deployed_model.predict([x])

        deployed_model_curl = deployed_model.get_curl()
        deployed_status = experiment_run.get_deployment_status()
        assert deployed_status["url"] in deployed_model_curl
        assert deployed_status["token"] in deployed_model_curl

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_not_deployed_get_error(self, experiment_run, model_for_deployment):
        with pytest.raises(RuntimeError):
            experiment_run.get_deployed_model()

    def test_undeployed_get_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        experiment_run.deploy(wait=True)
        experiment_run.undeploy(wait=True)

        with pytest.raises(RuntimeError):
            experiment_run.get_deployed_model()

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )


class TestGitOps:
    def test_download_deployment_yaml(self, experiment_run, model_for_deployment, in_tempdir):
        download_to_path = "deployment.yaml"

        experiment_run.log_model(
            model_for_deployment['model'],
            custom_modules=[],
            model_api=model_for_deployment['model_api'],
        )
        experiment_run.log_environment(Python(['scikit-learn']))

        filepath = experiment_run.download_deployment_yaml(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as YAML
        with open(filepath, 'rb') as f:
            model_deployment = yaml.safe_load(f)

        assert model_deployment['kind'] == "ModelDeployment"
        assert model_deployment['metadata']['name'] == experiment_run.id

    def test_download_docker_context(self, experiment_run, model_for_deployment, in_tempdir):
        download_to_path = "context.tgz"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        filepath = experiment_run.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, 'r:gz') as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths
