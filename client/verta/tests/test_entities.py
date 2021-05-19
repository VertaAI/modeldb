import six

import itertools
import os
import shutil

import requests

import pytest

from verta.registry.entities import RegisteredModels
from verta.tracking.entities._deployable_entity import _CACHE_DIR
from . import utils

import verta
import verta._internal_utils._utils
import json

from verta.external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from verta._protos.public.modeldb import ExperimentRunService_pb2 as _ExperimentRunService


KWARGS = {
    'desc': [None, "A test."],
    'tags': [None, ['test']],
    'attrs': [None, {'is_test': True}],
}
KWARGS_COMBOS = [dict(zip(KWARGS.keys(), values))
                 for values
                 in itertools.product(*KWARGS.values())
                 if values.count(None) != len(values)]

# for `tags` typecheck tests
TAG = "my-tag"


class TestLazyList:
    class LL(verta._internal_utils._utils.LazyList):
        def __init__(self, size):
            super(TestLazyList.LL, self).__init__(None, None, _ExperimentRunService.FindExperimentRuns())
            self._size = size
            self._calls = 0

        def _call_back_end(self, msg):
            self._calls += 1

            start = (self._page_number(msg)-1)*self._page_limit(msg)
            end = self._page_number(msg)*self._page_limit(msg)
            end = min(self._size, end)
            ids = list(range(start, end))
            objs = [_ExperimentRunService.ExperimentRun(id=str(i)) for i in ids]
            assert max(ids) <= self._size
            return objs, self._size

        def _create_element(self, el):
            return el

    def test_ll(self):
        size = 1000
        ll = TestLazyList.LL(size)

        for i, obj in enumerate(ll):
            assert str(i) == obj.id

    def test_limit(self):
        size = 1000
        limit = 10
        ll = TestLazyList.LL(size)
        ll.set_page_limit(limit)

        for i, obj in enumerate(ll):
            assert str(i) == obj.id

        assert ll._calls == size/limit


class TestClient:
    @pytest.mark.skipif('VERTA_EMAIL' not in os.environ or 'VERTA_DEV_KEY' not in os.environ, reason="insufficient Verta credentials")
    def test_auth_headers(self, host):
        expected_headers = {
            'Grpc-Metadata-email': os.environ['VERTA_EMAIL'],
            'Grpc-Metadata-developer_key': os.environ['VERTA_DEV_KEY'],
            'Grpc-Metadata-developer-key': os.environ['VERTA_DEV_KEY'],
        }
        def assert_contains_expected_headers(headers):
            for key, val in expected_headers.items():
                assert key in headers
                assert headers[key] == val

        # present in Client state
        client = verta.Client(host)
        assert_contains_expected_headers(client._conn.auth)

        # sent in requests
        with pytest.raises(requests.HTTPError) as exc_info:
            verta.Client("www.google.com")
        request = exc_info.value.request
        assert_contains_expected_headers(request.headers)

    @pytest.mark.oss
    def test_no_auth(self, host):
        EMAIL_KEY, DEV_KEY_KEY = "VERTA_EMAIL", "VERTA_DEV_KEY"
        EMAIL, DEV_KEY = os.environ.pop(EMAIL_KEY, None), os.environ.pop(DEV_KEY_KEY, None)
        try:
            client = verta.Client(host)

            # still has source set
            assert 'Grpc-Metadata-source' in client._conn.auth

            assert client.set_project()

            client.proj.delete()
        finally:
            if EMAIL is not None:
                os.environ[EMAIL_KEY] = EMAIL
            if DEV_KEY is not None:
                os.environ[DEV_KEY_KEY] = DEV_KEY


    @pytest.mark.skipif('VERTA_EMAIL' not in os.environ or 'VERTA_DEV_KEY' not in os.environ, reason="insufficient Verta credentials")
    def test_verta_https(self):
        hosts = [
            "app.verta.ai",
        ]

        for host in hosts:
            # https by default
            conn = verta.Client(host)._conn
            assert conn.scheme == "https"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

            # http if provided
            conn = verta.Client("http://{}".format(host))._conn
            assert conn.scheme == "http"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

            # https if provided
            conn = verta.Client("https://{}".format(host))._conn
            assert conn.scheme == "https"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

    def test_else_http(self):
        # test hosts must not redirect http to https
        hosts = [
            "www.google.com",
        ]

        for host in hosts:
            # http by default
            try:
                verta.Client(host, max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "http"
            else:
                raise RuntimeError("faulty test; expected error")

            # http if provided
            try:
                verta.Client("http://{}".format(host), max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "http"
            else:
                raise RuntimeError("faulty test; expected error")

            # https if provided
            try:
                verta.Client("https://{}".format(host), max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "https"
            else:
                raise RuntimeError("faulty test; expected error")

    @pytest.mark.skipif(not all(env_var in os.environ for env_var in ('VERTA_HOST', 'VERTA_EMAIL', 'VERTA_DEV_KEY')), reason="insufficient Verta credentials")
    def test_config_file(self):
        self.config_file_with_type_util(connect = False)

    @pytest.mark.skipif(not all(env_var in os.environ for env_var in ('VERTA_HOST', 'VERTA_EMAIL', 'VERTA_DEV_KEY')), reason="insufficient Verta credentials")
    def test_config_file_connect(self):
        self.config_file_with_type_util(connect = True)

    def config_file_with_type_util(self, connect):
        PROJECT_NAME = verta._internal_utils._utils.generate_default_name()
        DATASET_NAME = verta._internal_utils._utils.generate_default_name()
        EXPERIMENT_NAME = verta._internal_utils._utils.generate_default_name()
        CONFIG_FILENAME = "verta_config.json"

        HOST_KEY, EMAIL_KEY, DEV_KEY_KEY = "VERTA_HOST", "VERTA_EMAIL", "VERTA_DEV_KEY"

        HOST, EMAIL, DEV_KEY = os.environ[HOST_KEY], os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY]
        try:
            del os.environ[HOST_KEY], os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY]

            try:
                with open(CONFIG_FILENAME, 'w') as f:
                    json.dump(
                        {
                            'host': HOST,
                            'email': EMAIL, 'dev_key': DEV_KEY,
                            'project': PROJECT_NAME,
                            'experiment': EXPERIMENT_NAME,
                            'dataset': DATASET_NAME,
                        },
                        f,
                    )

                client = verta.Client(_connect=connect)
                conn = client._conn

                back_end_url = urlparse(HOST)
                socket = back_end_url.netloc + back_end_url.path.rstrip('/')

                assert conn.socket == socket
                assert conn.auth['Grpc-Metadata-email'] == EMAIL
                assert conn.auth['Grpc-Metadata-developer_key'] == DEV_KEY
                assert conn.auth['Grpc-Metadata-developer-key'] == DEV_KEY

                if connect:
                    try:
                        assert client.set_experiment_run()
                        assert client.proj.name == PROJECT_NAME
                        assert client.expt.name == EXPERIMENT_NAME
                    finally:
                        if client.proj is not None:
                            client.proj.delete()
                    dataset = client.set_dataset()
                    try:
                        assert dataset.name == DATASET_NAME
                    finally:
                        dataset.delete()
                else:
                    assert client._set_from_config_if_none(None, "project") == PROJECT_NAME
                    assert client._set_from_config_if_none(None, "experiment") == EXPERIMENT_NAME
                    assert client._set_from_config_if_none(None, "dataset") == DATASET_NAME

            finally:
                if os.path.exists(CONFIG_FILENAME):
                    os.remove(CONFIG_FILENAME)
        finally:
            os.environ[HOST_KEY], os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY] = HOST, EMAIL, DEV_KEY

    @pytest.mark.not_oss
    def test_wrong_credentials(self):
        EMAIL_KEY, DEV_KEY_KEY = "VERTA_EMAIL", "VERTA_DEV_KEY"
        old_email, old_dev_key = os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY]

        try:
            os.environ[EMAIL_KEY] = "abc@email.com"
            os.environ[DEV_KEY_KEY] = "def"

            with pytest.raises(requests.exceptions.HTTPError) as excinfo:
                verta.Client()

            excinfo_value = str(excinfo.value).strip()
            assert "401 Client Error" in excinfo_value
            assert "authentication failed; please check `VERTA_EMAIL` and `VERTA_DEV_KEY`" in excinfo_value
        finally:
            os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY] = old_email, old_dev_key

class TestEntities:
    def test_cache(self, client, strs):
        client.set_project()
        client.set_experiment()

        entities = (
            client.set_experiment_run(),
        )

        for entity in entities:
            filename = strs[0]
            filepath = os.path.join(_CACHE_DIR, filename)
            contents = six.ensure_binary(strs[1])

            assert not os.path.isfile(filepath)
            assert not entity._get_cached_file(filename)

            try:
                assert entity._cache_file(filename, contents) == filepath

                assert os.path.isfile(filepath)
                assert entity._get_cached_file(filename)

                with open(filepath, 'rb') as f:
                    assert f.read() == contents
            finally:
                shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_context(self, client, strs):
        strs = iter(strs)
        def assert_new_run_in_proj():
            assert client.get_or_create_experiment_run()._msg.project_id == proj.id
            assert client.create_experiment_run()._msg.project_id == proj.id

        proj = client.create_project()
        assert_new_run_in_proj()

        client.get_or_create_repository(next(strs)).delete()
        # client.create_repository(next(strs)).delete()  # method DNE
        assert_new_run_in_proj()

        client.get_or_create_registered_model().delete()
        client.create_registered_model().delete()
        assert_new_run_in_proj()

        client.get_or_create_dataset().delete()
        client.create_dataset().delete()
        assert_new_run_in_proj()

        assert client.get_or_create_experiment()._msg.project_id == proj.id
        assert client.create_experiment()._msg.project_id == proj.id
        assert_new_run_in_proj()


class TestProject:
    def test_create(self, client):
        assert client.set_project()
        assert client.proj is not None
        name = verta._internal_utils._utils.generate_default_name()
        assert client.create_project(name)
        assert client.proj is not None
        with pytest.raises(requests.HTTPError) as excinfo:
            assert client.create_project(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client.get_or_create_project(name=name, tags=["tag1", "tag2"])

    def test_get(self, client):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_project(name)

        proj = client.set_project(name)

        assert proj.id == client.get_project(proj.name).id
        assert proj.id == client.get_project(id=proj.id).id

    def test_get_by_name(self, client):
        proj = client.set_project()

        client.set_project()  # in case get erroneously fetches latest

        assert proj.id == client.set_project(proj.name).id

    def test_get_by_id(self, client):
        proj = client.set_project()

        client.set_project()  # in case get erroneously fetches latest

        assert proj.id == client.set_project(id=proj.id).id

    def test_get_nonexistent_id(self, client):
        with pytest.raises(ValueError):
            client.set_project(id="nonexistent_id")

    @pytest.mark.parametrize("tags", [TAG, [TAG]])
    def test_tags_is_list_of_str(self, client, tags):
        proj = client.set_project(tags=tags)

        endpoint = "{}://{}/api/v1/modeldb/project/getProjectTags".format(
            client._conn.scheme,
            client._conn.socket,
        )
        response = verta._internal_utils._utils.make_request("GET", endpoint, client._conn, params={'id': proj.id})
        verta._internal_utils._utils.raise_for_http_error(response)
        assert response.json().get('tags', []) == [TAG]


class TestExperiment:
    def test_create(self, client):
        client.set_project()
        assert client.set_experiment()
        assert client.expt is not None

        name = verta._internal_utils._utils.generate_default_name()
        assert client.create_experiment(name)
        assert client.expt is not None
        with pytest.raises(requests.HTTPError) as excinfo:
            assert client.create_experiment(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client.set_experiment(name=name, attrs={"a": 123})

    def test_get(self, client):
        proj = client.set_project()
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_experiment(name)

        expt = client.set_experiment(name)

        assert expt.id == client.get_experiment(expt.name).id
        assert expt.id == client.get_experiment(id=expt.id).id

        # test parents are restored
        client.set_project()
        client.get_experiment(id=expt.id)
        assert client.proj.id == proj.id
        assert client.expt.id == expt.id

    def test_get_by_name(self, client):
        client.set_project()
        expt = client.set_experiment()

        client.set_experiment()  # in case get erroneously fetches latest

        assert expt.id == client.set_experiment(expt.name).id

    def test_get_by_id(self, client):
        proj = client.set_project()
        expt = client.set_experiment()

        client.set_experiment()  # in case get erroneously fetches latest

        assert expt.id == client.set_experiment(id=expt.id).id
        assert proj.id == client.proj.id

    def test_get_nonexistent_id_error(self, client):
        with pytest.raises(ValueError):
            client.set_experiment(id="nonexistent_id")

    @pytest.mark.parametrize("tags", [TAG, [TAG]])
    def test_tags_is_list_of_str(self, client, tags):
        client.set_project()
        expt = client.set_experiment(tags=tags)

        endpoint = "{}://{}/api/v1/modeldb/experiment/getExperimentTags".format(
            client._conn.scheme,
            client._conn.socket,
        )
        response = verta._internal_utils._utils.make_request("GET", endpoint, client._conn, params={'id': expt.id})
        verta._internal_utils._utils.raise_for_http_error(response)
        assert response.json().get('tags', []) == [TAG]


class TestExperimentRun:
    def test_create(self, client):
        client.set_project()
        client.set_experiment()

        assert client.set_experiment_run()

        name = verta._internal_utils._utils.generate_default_name()
        assert client.create_experiment_run(name)
        with pytest.raises(requests.HTTPError) as excinfo:
            assert client.create_experiment_run(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client.set_experiment_run(name=name, attrs={"a": 123})

    def test_get(self, client):
        proj = client.set_project()
        expt = client.set_experiment()
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_experiment_run(name)

        run = client.set_experiment_run(name)

        assert run.id == client.get_experiment_run(run.name).id
        assert run.id == client.get_experiment_run(id=run.id).id

        # test parents are restored by first setting new, unrelated ones
        client.set_project()
        client.set_experiment()
        client.get_experiment_run(id=run.id)
        assert client.proj.id == proj.id
        assert client.expt.id == expt.id

    def test_get_by_name(self, client):
        client.set_project()
        client.set_experiment()
        run = client.set_experiment_run()
        client.set_experiment_run()  # in case get erroneously fetches latest

        assert run.id == client.set_experiment_run(run.name).id

    def test_get_by_id(self, client):
        proj = client.set_project()
        expt = client.set_experiment()
        expt_run = client.set_experiment_run()

        client.set_experiment_run()  # in case get erroneously fetches latest

        assert expt_run.id == client.set_experiment_run(id=expt_run.id).id
        assert proj.id == client.proj.id
        assert expt.id == client.expt.id

    def test_get_nonexistent_id_error(self, client):
        with pytest.raises(ValueError):
            client.set_experiment_run(id="nonexistent_id")

    def test_no_experiment_error(self, client):
        with pytest.raises(AttributeError):
            client.set_experimennt_run()

    @pytest.mark.parametrize("tags", [TAG, [TAG]])
    def test_tags_is_list_of_str(self, client, tags):
        client.set_project()
        client.set_experiment()
        run = client.set_experiment_run(tags=tags)

        endpoint = "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunTags".format(
            client._conn.scheme,
            client._conn.socket,
        )
        response = verta._internal_utils._utils.make_request("GET", endpoint, client._conn, params={'id': run.id})
        verta._internal_utils._utils.raise_for_http_error(response)
        assert response.json().get('tags', []) == [TAG]

    def test_clone(self, experiment_run):
        expt_run = experiment_run
        expt_run._conf.use_git = False
        expt_run.log_hyperparameters({"hpp1" : 1, "hpp2" : 2, "hpp3" : "hpp3"})
        expt_run.log_metrics({"metric1" : 0.5, "metric2" : 0.6})
        expt_run.log_tags(["tag1", "tag2"])
        expt_run.log_attributes({"attr1" : 10, "attr2" : {"abc": 1}})
        expt_run.log_artifact("my-artifact", "README.md")
        expt_run.log_code()

        # set various things in the run
        new_run_no_art = expt_run.clone()

        old_run_msg = expt_run._get_proto_by_id(expt_run._conn, expt_run.id)
        new_run_no_art_msg = new_run_no_art._get_proto_by_id(new_run_no_art._conn, new_run_no_art.id)

        # ensure basic data is the same
        assert expt_run.id != new_run_no_art_msg.id
        assert old_run_msg.description == new_run_no_art_msg.description
        assert old_run_msg.tags == new_run_no_art_msg.tags
        assert old_run_msg.metrics == new_run_no_art_msg.metrics
        assert old_run_msg.hyperparameters == new_run_no_art_msg.hyperparameters
        assert old_run_msg.observations == new_run_no_art_msg.observations
        assert old_run_msg.artifacts == new_run_no_art_msg.artifacts


    def test_clone_into_expt(self, client):
        expt1 = client.set_experiment()

        expt2 = client.set_experiment()
        assert expt1.id != expt2.id  # of course, but just to be sure

        old_run = client.set_experiment_run()
        assert old_run._msg.experiment_id == expt2.id  # of course, but just to be sure

        old_run.log_hyperparameters({"hpp1" : 1, "hpp2" : 2, "hpp3" : "hpp3"})
        old_run.log_metrics({"metric1" : 0.5, "metric2" : 0.6})
        old_run.log_tags(["tag1", "tag2"])
        old_run.log_attributes({"attr1" : 10, "attr2" : {"abc": 1}})
        old_run.log_artifact("my-artifact", "README.md")

        new_run = old_run.clone(experiment_id=expt1.id)

        old_run_msg = old_run._get_proto_by_id(old_run._conn, old_run.id)
        new_run_msg = new_run._get_proto_by_id(new_run._conn, new_run.id)

        assert old_run_msg.id != new_run_msg.id
        assert new_run_msg.experiment_id == expt1.id

        assert old_run_msg.description == new_run_msg.description
        assert old_run_msg.tags == new_run_msg.tags
        assert old_run_msg.metrics == new_run_msg.metrics
        assert old_run_msg.hyperparameters == new_run_msg.hyperparameters
        assert old_run_msg.observations == new_run_msg.observations
        assert old_run_msg.artifacts == new_run_msg.artifacts

    def test_log_attribute_overwrite(self, client):
        initial_attrs = {"str-attr": "attr", "int-attr": 4, "float-attr": 0.5}
        new_attrs = {"str-attr": "new-attr", "int-attr": 5, "float-attr": 0.3, "bool-attr": False}
        single_new_attr = new_attrs.popitem()

        experiment_run = client.set_experiment_run(attrs=initial_attrs)

        with pytest.raises(ValueError) as excinfo:
            experiment_run.log_attribute("str-attr", "some-attr")

        assert "already exists" in str(excinfo.value)

        experiment_run.log_attribute(*single_new_attr, overwrite=True)
        experiment_run.log_attributes(new_attrs, True)

        expected_attrs = initial_attrs.copy()
        expected_attrs.update([single_new_attr])
        expected_attrs.update(new_attrs)

        assert experiment_run.get_attributes() == expected_attrs


class TestExperimentRuns:
    def test_getitem(self, client):
        client.set_project()
        expt_runs = client.set_experiment().expt_runs

        local_run_ids = set(client.set_experiment_run().id for _ in range(3))

        assert expt_runs[1].id in local_run_ids

    def test_negative_indexing(self, client):
        client.set_project()
        expt_runs = client.set_experiment().expt_runs

        local_run_ids = set(client.set_experiment_run().id for _ in range(3))

        assert expt_runs[-1].id in local_run_ids

    def test_index_out_of_range_error(self, client):
        client.set_project()
        expt_runs = client.set_experiment().expt_runs

        [client.set_experiment_run() for _ in range(3)]

        with pytest.raises(IndexError):
            expt_runs[6]

        with pytest.raises(IndexError):
            expt_runs[-6]

    def test_iter(self, client):
        client.set_project()
        expt_runs = client.set_experiment().expt_runs

        expt_runs._ITER_PAGE_LIMIT = 3

        local_run_ids = set(client.set_experiment_run().id for _ in range(6))

        # iterate through all 6 runs
        assert local_run_ids == set(run.id for run in expt_runs)

        # don't fail ungracefully while runs are added
        for i, _ in enumerate(expt_runs):
            if i == 4:
                [client.set_experiment_run() for _ in range(3)]

    def test_len(self, client):
        client.set_project()
        expt_runs = client.set_experiment().expt_runs

        assert len([client.set_experiment_run().id for _ in range(3)]) == len(expt_runs)

    def test_as_dataframe(self, client, strs):
        np = pytest.importorskip("numpy")
        pytest.importorskip("pandas")

        # initialize entities
        client.set_project()
        expt = client.set_experiment()
        for _ in range(3):
            client.set_experiment_run()

        # log metadata
        hpp1, hpp2, metric1, metric2 = strs[:4]
        for run in expt.expt_runs:
            run.log_hyperparameters({
                hpp1: np.random.random(),
                hpp2: np.random.random(),
            })
            run.log_metrics({
                metric1: np.random.random(),
                metric2: np.random.random(),
            })

        # verify that DataFrame matches
        df = expt.expt_runs.as_dataframe()
        assert set(df.index) == set(run.id for run in expt.expt_runs)
        for run in expt.expt_runs:
            row = df.loc[run.id]
            assert row['hpp.'+hpp1] == run.get_hyperparameter(hpp1)
            assert row['hpp.'+hpp2] == run.get_hyperparameter(hpp2)
            assert row['metric.'+metric1] == run.get_metric(metric1)
            assert row['metric.'+metric2] == run.get_metric(metric2)

    def test_find(self, client):
        client.set_project()
        expt = client.set_experiment()
        tag = "some-tag"
        diff_tag = "diff-tag"

        run_with_diff_tag = client.set_experiment_run("run-with-diff-tag")
        run_with_diff_tag.log_tag(diff_tag)

        runs_with_tag = []
        for _ in range(5):
            runs_with_tag.append(client.set_experiment_run())
            runs_with_tag[-1].log_tag(tag)

        found_runs = expt.expt_runs.find("tags ~= {}".format(tag))
        assert len(found_runs) == len(runs_with_tag)

        runs_with_tag[-1].log_hyperparameter("some-hyper", 1)
        # compound conditions:
        assert len(expt.expt_runs.find(["tags ~= {}".format(tag), "hyperparameters.some-hyper == 1"])) == 1  # old syntax
        assert len(expt.expt_runs.find("tags ~= {}".format(tag), "hyperparameters.some-hyper == 1")) == 1  # new syntax

        # if any predicate is not string, should fail:
        with pytest.raises(TypeError, match="predicates must all be strings"):
            expt.expt_runs.find("tag ~= {}".format(tag), 1234)


    @pytest.mark.skip("functionality removed")
    def test_add(self, client):
        client.set_project()
        expt1 = client.set_experiment()
        local_expt1_run_ids = set(client.set_experiment_run().id for _ in range(3))
        expt2 = client.set_experiment()
        local_expt2_run_ids = set(client.set_experiment_run().id for _ in range(3))

        # simple concatenation
        assert local_expt1_run_ids | local_expt2_run_ids == set(run.id for run in expt1.expt_runs + expt2.expt_runs)

        # ignore duplicates
        assert local_expt1_run_ids == set(run.id for run in expt1.expt_runs + expt1.expt_runs)
