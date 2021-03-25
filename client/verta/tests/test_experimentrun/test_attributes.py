import pytest

from verta import attributes


class TestCustomAttributes:  # TODO: sort methods alphabetically
    def test_string_value(self):
        attr = attributes.StringValue("umbrella")
        assert attr._as_dict() == {
            "type": "verta.stringValue.v1",
            "stringValue": {
                "value": "umbrella",
            },
        }

    def test_numeric_value(self):
        attr = attributes.NumericValue(42)
        assert attr._as_dict() == {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 42,
            },
        }

    def test_numeric_value_with_unit(self):
        attr = attributes.NumericValue(14, unit="lbs")
        assert attr._as_dict() == {
            "type": "verta.numericValue.v1",
            "numericValue": {
                "value": 14,
                "unit": "lbs",
            },
        }

    def test_discrete_histogram(self):
        attr = attributes.DiscreteHistogram(
            buckets=["yes", "no"],
            data=[10, 20],
        )
        assert attr._as_dict() == {
            "type": "verta.discreteHistogram.v1",
            "discreteHistogram": {
                "buckets": ["yes", "no"],
                "data": [10, 20],
            },
        }

    def test_float_histogram(self):
        attr = attributes.FloatHistogram(
            bucket_limits=[0, 3, 6],
            data=[10, 20],
        )
        assert attr._as_dict() == {
            "type": "verta.floatHistogram.v1",
            "floatHistogram": {
                "bucketLimits": [0, 3, 6],
                "data": [10, 20],
            },
        }

    def test_table(self):
        attr = attributes.Table(
            data=[[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        assert attr._as_dict() == {
            "type": "verta.table.v1",
            "table": {
                "header": ["header1", "header2", "header3"],
                "rows": [[1, "two", 3], [4, "five", 6]],
            },
        }

    def test_table_numpy(self):
        np = pytest.importorskip("numpy")
        attr = attributes.Table(
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

    def test_table_from_pandas(self):
        pd = pytest.importorskip("pandas")
        df = pd.DataFrame(
            [[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        attr = attributes.Table.from_pandas(df)
        assert attr._as_dict() == {
            "type": "verta.table.v1",
            "table": {
                "header": ["header1", "header2", "header3"],
                "rows": [[1, "two", 3], [4, "five", 6]],
            },
        }

    def test_matrix(self):
        attr = attributes.Matrix([[1, 2, 3], [4, 5, 6]])
        assert attr._as_dict() == {
            "type": "verta.matrix.v1",
            "matrix": {
                "value": [[1, 2, 3], [4, 5, 6]],
            },
        }

    def test_matrix_numpy(self):
        np = pytest.importorskip("numpy")
        attr = attributes.Matrix(np.arange(1, 7).reshape((2, 3)))
        assert attr._as_dict() == {
            "type": "verta.matrix.v1",
            "matrix": {
                "value": [[1, 2, 3], [4, 5, 6]],
            },
        }

    def test_series(self):
        attr = attributes.Series([1, 2, 3])
        assert attr._as_dict() == {
            "type": "verta.series.v1",
            "series": {
                "value": [1, 2, 3],
            },
        }

    def test_series_numpy(self):
        np = pytest.importorskip("numpy")
        attr = attributes.Series(np.arange(1, 4))
        assert attr._as_dict() == {
            "type": "verta.series.v1",
            "series": {
                "value": [1, 2, 3],
            },
        }

    def test_line(self):
        attr = attributes.Line(
            x=[1, 2, 3],
            y=[1, 4, 9],
        )
        assert attr._as_dict() == {
            "type": "verta.line.v1",
            "line": {
                "x": [1, 2, 3],
                "y": [1, 4, 9],
            },
        }

    def test_line_from_tuples(self):
        attr = attributes.Line.from_tuples(
            [(1, 1), (2, 4), (3, 9)]
        )
        assert attr._as_dict() == {
            "type": "verta.line.v1",
            "line": {
                "x": [1, 2, 3],
                "y": [1, 4, 9],
            },
        }

    def test_confusion_matrix(self):
        attr = attributes.ConfusionMatrix(
            value=[[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            labels=["a", "b", "c"],
        )
        assert attr._as_dict() == {
            "type": "verta.confusionMatrix.v1",
            "confusionMatrix": {
                "labels": ["a", "b", "c"],
                "value": [[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            },
        }

    def test_confusion_matrix_numpy(self):
        np = pytest.importorskip("numpy")
        attr = attributes.ConfusionMatrix(
            value=np.arange(1, 10).reshape((3, 3)),
            labels=["a", "b", "c"],
        )
        assert attr._as_dict() == {
            "type": "verta.confusionMatrix.v1",
            "confusionMatrix": {
                "labels": ["a", "b", "c"],
                "value": [[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            },
        }
