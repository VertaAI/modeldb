# pylint: disable=unidiomatic-typecheck

import six

import pathlib2
import subprocess
import sys

import verta
from verta._internal_utils import _utils
from verta._internal_utils import _file_utils
from verta._internal_utils import _pip_requirements_utils

import pytest
from . import utils


class TestMakeRequest:
    def test_200_no_history(self, client):
        """
        The util manually tracks and assigns the response history after resolving redirects.

        If no redirects occurred, the history should be empty.

        """
        response = _utils.make_request(
            "GET",
            "http://httpbin.org/status/200",
            client._conn,
        )

        assert response.status_code == 200
        assert not response.history

    # https://github.com/postmanlabs/httpbin/issues/617
    @pytest.mark.skip(reason="httpbin's /redirect-to is currently returning 404s")
    def test_301_continue(self, client):
        response = _utils.make_request(
            "GET",
            "http://httpbin.org/redirect-to",
            client._conn,
            params={
                'url': "http://httpbin.org/get",
                'status_code': 301,
            },
        )

        assert response.status_code == 200
        assert len(response.history) == 1
        assert response.history[0].status_code == 301

    # https://github.com/postmanlabs/httpbin/issues/617
    @pytest.mark.skip(reason="httpbin's /redirect-to is currently returning 404s")
    def test_302_stop(self, client):
        with pytest.raises(RuntimeError) as excinfo:
            _utils.make_request(
                "GET",
                "http://httpbin.org/redirect-to",
                client._conn,
                params={
                    'url': "http://httpbin.org/get",
                    'status_code': 302,
                },
            )
        assert str(excinfo.value).strip().startswith("received status 302")

    @pytest.mark.parametrize("status_code", [302, 400, 500])
    def test_ignore_conn_err(self, client, status_code):
        previous_setting = client.ignore_conn_err

        client.ignore_conn_err = True
        try:
            response = _utils.make_request(
                "GET",
                "http://httpbin.org/status/{}".format(status_code),
                client._conn,
            )

            assert response.status_code == 200
            assert response.json() == {}
        finally:
            client.ignore_conn_err = previous_setting


class TestBodyToJson:
    def test_json_response(self, client):
        response = _utils.make_request(
            "GET",
            "http://httpbin.org/json",
            client._conn,
        )

        assert isinstance(_utils.body_to_json(response), dict)

    def test_empty_response_error(self, client):
        response = _utils.make_request(
            "GET",
            "http://httpbin.org/status/200",
            client._conn,
        )

        with pytest.raises(ValueError) as excinfo:
            _utils.body_to_json(response)
        msg = str(excinfo.value).strip()
        assert msg.startswith("expected JSON response")
        assert "<empty response>" in msg

    def test_html_response_error(self, client):
        response = _utils.make_request(
            "GET",
            "http://httpbin.org/html",
            client._conn,
        )

        with pytest.raises(ValueError) as excinfo:
            _utils.body_to_json(response)
        msg = str(excinfo.value).strip()
        assert msg.startswith("expected JSON response")
        assert "<!DOCTYPE html>" in msg


class TestToBuiltin:
    def test_string(self):
        val = "banana"

        assert _utils.to_builtin(val) == "banana"

    def test_unicode(self):
        val = u"banana"

        assert _utils.to_builtin(val) == "banana"

    def test_bytes(self):
        val = b"banana"

        assert _utils.to_builtin(val) == "banana"

    def test_numpy_numbers(self):
        np = pytest.importorskip("numpy")

        ints = (
            np.int8(), np.int16(), np.int32(), np.int64(),
            np.uint8(), np.uint16(), np.uint32(), np.uint64(),
        )
        floats = (
            np.float32(), np.float64(),
        )

        for val in ints:
            assert type(_utils.to_builtin(val)) in six.integer_types

        for val in floats:
            assert type(_utils.to_builtin(val)) is float

    def test_ndarray(self):
        np = pytest.importorskip("numpy")

        int_array = np.random.randint(-36, 36, size=(12, 24))
        float_array = np.random.uniform(-36, 36, size=(12, 24))
        str_array = np.array([list("banana"), list("coconut"), list("date")])

        builtin_int_array = _utils.to_builtin(int_array)
        assert type(builtin_int_array) is list
        assert all(type(val) in six.integer_types
                   for row in builtin_int_array
                   for val in row)

        builtin_float_array = _utils.to_builtin(float_array)
        assert type(builtin_float_array) is list
        assert all(type(val) is float
                   for row in builtin_float_array
                   for val in row)

        builtin_str_array = _utils.to_builtin(str_array)
        assert type(builtin_str_array) is list
        assert all(type(val) is str
                   for row in builtin_str_array
                   for val in row)

    def test_series(self):
        pd = pytest.importorskip("pandas")

        int_series = pd.Series([1, 2, 3])
        float_series = pd.Series([1.0, 2.0, 3.0])
        str_series = pd.Series(["one", "two", "thr"])

        builtin_int_series = _utils.to_builtin(int_series)
        assert type(builtin_int_series) is list
        assert all(type(val) in six.integer_types for val in builtin_int_series)

        builtin_float_series = _utils.to_builtin(float_series)
        assert type(builtin_float_series) is list
        assert all(type(val) is float for val in builtin_float_series)

        builtin_str_series = _utils.to_builtin(str_series)
        assert type(builtin_str_series) is list
        assert all(type(val) is str for val in builtin_str_series)


    def test_dataframe(self):
        pd = pytest.importorskip("pandas")

        int_frame = pd.DataFrame([[1, 1, 1],
                                  [2, 2, 2],
                                  [3, 3, 3]])
        float_frame = pd.DataFrame([[1.0, 1.0, 1.0],
                                    [2.0, 2.0, 2.0],
                                    [3.0, 3.0, 3.0]])
        str_frame = pd.DataFrame([["one", "one", "one"],
                                  ["two", "two", "two"],
                                  ["thr", "thr", "thr"]])

        builtin_int_frame = _utils.to_builtin(int_frame)
        assert type(builtin_int_frame) is list
        assert all(type(val) in six.integer_types
                   for row in builtin_int_frame
                   for val in row)

        builtin_float_frame = _utils.to_builtin(float_frame)
        assert type(builtin_float_frame) is list
        assert all(type(val) is float
                   for row in builtin_float_frame
                   for val in row)

        builtin_str_frame = _utils.to_builtin(str_frame)
        assert type(builtin_str_frame) is list
        assert all(type(val) is str
                   for row in builtin_str_frame
                   for val in row)

    def test_dict(self):
        np = pytest.importorskip("numpy")

        val = {
            "banana": np.array([1, 2, 3]),
            u"coconut": np.array([1.0, 2.0, 3.0]),
            b"date": np.array(list("banana")),
        }

        builtin_val = _utils.to_builtin(val)

        assert set(builtin_val.keys()) == {"banana", "coconut", "date"}
        assert builtin_val['banana'] == [1, 2, 3]
        assert builtin_val['coconut'] == [1.0, 2.0, 3.0]
        assert builtin_val['date'] == list("banana")

    def test_list(self):
        np = pytest.importorskip("numpy")

        int_list = list(np.array([1, 2, 3]))
        float_list = list(np.array([1.0, 2.0, 3.0]))
        str_list = list(np.array(list("banana")))

        assert not any(type(val) in six.integer_types for val in int_list)
        assert not any(type(val) is float for val in float_list)
        assert not any(type(val) is str for val in str_list)

        builtin_int_list = _utils.to_builtin(int_list)
        assert type(builtin_int_list) is list
        assert all(type(val) in six.integer_types for val in builtin_int_list)

        builtin_float_list = _utils.to_builtin(float_list)
        assert type(builtin_float_list) is list
        assert all(type(val) is float for val in builtin_float_list)

        builtin_str_list = _utils.to_builtin(str_list)
        assert type(builtin_str_list) is list
        assert all(type(val) is str for val in builtin_str_list)


class TestPipRequirementsUtils:
    def test_no_spacy_models_in_pip_freeze(self):
        spacy = pytest.importorskip("spacy")
        try:
            spacy.load("en_core_web_sm")
        except OSError:
            pytest.skip("SpaCy en_core_web_sm model not installed")

        # baseline: en_core_web_sm in pip freeze
        assert list(filter(
            _pip_requirements_utils.SPACY_MODEL_REGEX.match,
            six.ensure_str(
                subprocess.check_output([sys.executable, '-m', 'pip', 'freeze']),
            ).splitlines(),
        ))

        # en_core_web_sm not in our pip freeze util
        assert not list(filter(
            _pip_requirements_utils.SPACY_MODEL_REGEX.match,
            _pip_requirements_utils.get_pip_freeze(),
        ))


class TestFileUtils:
    @pytest.mark.parametrize(
        "input_filepath, expected_filepath",
        [
            ("data.csv", "data 1.csv"),
            ("data 1.csv", "data 2.csv"),
            ("my data.csv", "my data 1.csv"),
            ("my data 1.csv", "my data 2.csv"),
            ("archive.tar.gz", "archive.tar 1.gz"),
            ("archive.tar 1.gz", "archive.tar 2.gz"),
            ("my archive.tar.gz", "my archive.tar 1.gz"),
            ("my archive.tar 1.gz", "my archive.tar 2.gz"),
        ],
    )
    def test_increment_path(self, input_filepath, expected_filepath):
        assert expected_filepath == _file_utils.increment_path(input_filepath)

    @pytest.mark.parametrize(
        "path, prefix_dir, expected",
        [
            # simple removal cases
            ("files/data.csv",      "files",      "data.csv"),
            ("files/data/data.csv", "files/data", "data.csv"),
            # simple no-change cases
            ("files/data.csv",      "foo",            "files/data.csv"),
            ("files/data.csv",      "fil",            "files/data.csv"),
            ("files/data/data.csv", "files/data.csv", "files/data/data.csv"),
            # edge cases
            ("data.csv", "data.csv", "data.csv"),
            # examples from comments in fn
            ("data/census-train.csv", "data/census", "data/census-train.csv"),
            ("data/census/train.csv", "data/census", "train.csv"),
            # remove "s3://"
            ("s3://verta-starter/census-train.csv", "s3:",   "verta-starter/census-train.csv"),
            ("s3://verta-starter/census-train.csv", "s3:/",  "verta-starter/census-train.csv"),
            ("s3://verta-starter/census-train.csv", "s3://", "verta-starter/census-train.csv"),
        ]
    )
    def test_remove_prefix_dir(self, path, prefix_dir, expected):
        assert _file_utils.remove_prefix_dir(path, prefix_dir) == expected

    def test_flatten_file_trees(self, in_tempdir):
        filepaths = {
            "README.md",
            "data/train.csv",
            "data/test.csv",
            "script.py",
            "utils/data/clean.py",
            "utils/misc/misc.py",
        }
        paths = ["README.md", "data", "script.py", "utils"]

        # create files
        for filepath in filepaths:
            filepath = pathlib2.Path(filepath)
            filepath.parent.mkdir(parents=True, exist_ok=True)
            filepath.touch()

        assert _file_utils.flatten_file_trees(paths) == filepaths
