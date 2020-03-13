# pylint: disable=unidiomatic-typecheck

import six

import verta
from verta._internal_utils import _utils

import pytest
from . import utils


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
