# -*- coding: utf-8 -*-

import copy

import pytest
from verta import data_types
from verta._internal_utils import importer


if importer.maybe_dependency("scipy") is None:
    pytest.skip("scipy is not installed", allow_module_level=True)


class TestConfusionMatrix:
    def test_confusion_matrix(self):
        attr = data_types.ConfusionMatrix(
            value=[[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            labels=["a", "b", "c"],
        )
        d = {
            "type": "verta.confusionMatrix.v1",
            "confusionMatrix": {
                "labels": ["a", "b", "c"],
                "value": [[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.ConfusionMatrix(
            value=np.arange(1, 10).reshape((3, 3)),
            labels=np.array(["a", "b", "c"]),
        )
        assert attr._as_dict() == {
            "type": "verta.confusionMatrix.v1",
            "confusionMatrix": {
                "labels": ["a", "b", "c"],
                "value": [[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            },
        }


class TestDiscreteHistogram:
    def test_discrete_histogram(self):
        attr = data_types.DiscreteHistogram(
            buckets=["yes", "no"],
            data=[10, 20],
        )
        d = {
            "type": "verta.discreteHistogram.v1",
            "discreteHistogram": {
                "buckets": ["yes", "no"],
                "data": [10, 20],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.DiscreteHistogram(
            buckets=np.array(["yes", "no"]),
            data=np.array([10, 20]),
        )
        assert attr._as_dict() == {
            "type": "verta.discreteHistogram.v1",
            "discreteHistogram": {
                "buckets": ["yes", "no"],
                "data": [10, 20],
            },
        }

    def test_missing_buckets(self):
        np = pytest.importorskip("numpy")
        one = data_types.DiscreteHistogram(
            buckets=["a", "b"],
            data=[0, 1],
        )
        two = data_types.DiscreteHistogram(
            buckets=["c", "a"],
            data=[1, 0],
        )
        d = one.diff(two)
        np.isclose(d, 0)


class TestFloatHistogram:
    def test_float_histogram(self):
        attr = data_types.FloatHistogram(
            bucket_limits=[0, 3, 6],
            data=[10, 20],
        )
        d = {
            "type": "verta.floatHistogram.v1",
            "floatHistogram": {
                "bucketLimits": [0, 3, 6],
                "data": [10, 20],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.FloatHistogram(
            bucket_limits=np.array([0, 3, 6]),
            data=np.array([10, 20]),
        )
        assert attr._as_dict() == {
            "type": "verta.floatHistogram.v1",
            "floatHistogram": {
                "bucketLimits": [0, 3, 6],
                "data": [10, 20],
            },
        }


class TestLine:
    def test_line(self):
        attr = data_types.Line(
            x=[1, 2, 3],
            y=[1, 4, 9],
        )
        d = {
            "type": "verta.line.v1",
            "line": {
                "x": [1, 2, 3],
                "y": [1, 4, 9],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.Line(
            x=np.array([1, 2, 3]),
            y=np.array([1, 4, 9]),
        )
        assert attr._as_dict() == {
            "type": "verta.line.v1",
            "line": {
                "x": [1, 2, 3],
                "y": [1, 4, 9],
            },
        }

    def test_from_tuples(self):
        attr = data_types.Line.from_tuples([(1, 1), (2, 4), (3, 9)])
        assert attr._as_dict() == {
            "type": "verta.line.v1",
            "line": {
                "x": [1, 2, 3],
                "y": [1, 4, 9],
            },
        }


class TestMatrix:
    def test_matrix(self):
        attr = data_types.Matrix([[1, 2, 3], [4, 5, 6]])
        d = {
            "type": "verta.matrix.v1",
            "matrix": {
                "value": [[1, 2, 3], [4, 5, 6]],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.Matrix(np.arange(1, 7).reshape((2, 3)))
        assert attr._as_dict() == {
            "type": "verta.matrix.v1",
            "matrix": {
                "value": [[1, 2, 3], [4, 5, 6]],
            },
        }


class TestNumericValue:
    def test_numeric_value(self):
        attr = data_types.NumericValue(42)
        d = {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 42,
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_with_unit(self):
        attr = data_types.NumericValue(14, unit="lbs")
        d = {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 14,
                "unit": "lbs",
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_from_dict_no_unit(self):
        d1 = {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 6,
            },
        }
        d2 = copy.deepcopy(d1)
        d2["numericValue"]["unit"] = ""
        d3 = copy.deepcopy(d1)
        d2["numericValue"]["unit"] = None

        attr1 = data_types._VertaDataType._from_dict(d1)
        attr2 = data_types._VertaDataType._from_dict(d2)
        attr3 = data_types._VertaDataType._from_dict(d3)

        for attr in [attr1, attr2, attr3]:
            assert isinstance(attr, data_types.NumericValue)
        assert attr1 == attr2 == attr3

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        d = {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 42,
            },
        }

        attr = data_types.NumericValue(np.float32(42))
        assert attr._as_dict() == d

        attr = data_types.NumericValue(np.array(42))
        assert attr._as_dict() == d


class TestStringValue:
    def test_string_value(self):
        attr = data_types.StringValue("umbrella")
        d = {
            "type": "verta.stringValue.v1",
            "stringValue": {
                "value": "umbrella",
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)


class TestTable:
    def test_table(self):
        attr = data_types.Table(
            data=[[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        d = {
            "type": "verta.table.v1",
            "table": {
                "header": ["header1", "header2", "header3"],
                "rows": [[1, "two", 3], [4, "five", 6]],
            },
        }
        assert attr._as_dict() == d
        assert attr == data_types._VertaDataType._from_dict(d)

    def test_numpy(self):
        np = pytest.importorskip("numpy")
        attr = data_types.Table(
            data=np.arange(1, 7).reshape((2, 3)),
            columns=["header1", "header2", "header3"],
        )
        assert attr._as_dict() == {
            "type": "verta.table.v1",
            "table": {
                "header": ["header1", "header2", "header3"],
                "rows": [[1, 2, 3], [4, 5, 6]],
            },
        }

    def test_from_pandas(self):
        pd = pytest.importorskip("pandas")
        df = pd.DataFrame(
            [[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        attr = data_types.Table.from_pandas(df)
        assert attr._as_dict() == {
            "type": "verta.table.v1",
            "table": {
                "header": ["header1", "header2", "header3"],
                "rows": [[1, "two", 3], [4, "five", 6]],
            },
        }
