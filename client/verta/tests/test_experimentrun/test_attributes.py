import string

import pytest
import six

from verta._internal_utils import _utils, importer
from verta import data_types


class TestSimpleAttributes:
    def test_keys(self, experiment_run):
        keys = (c for c in string.printable if c not in _utils._VALID_FLAT_KEY_CHARS)
        for key in keys:
            with pytest.raises(ValueError):
                experiment_run.log_attribute(key, 'key test')

    def test_single(self, experiment_run, strs, all_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        attributes = dict(zip(strs, all_values))

        for key, val in six.viewitems(attributes):
            experiment_run.log_attribute(key, val)

        with pytest.raises(KeyError):
            experiment_run.get_attribute(holdout)

        for key, val in six.viewitems(attributes):
            assert experiment_run.get_attribute(key) == val

        assert experiment_run.get_attributes() == attributes

    def test_batch(self, experiment_run, strs, all_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        attributes = dict(zip(strs, all_values))

        experiment_run.log_attributes(attributes)

        with pytest.raises(KeyError):
            experiment_run.get_attribute(holdout)

        for key, val in six.viewitems(attributes):
            assert experiment_run.get_attribute(key) == val

        assert experiment_run.get_attributes() == attributes

    def test_conflict(self, experiment_run, strs, all_values):
        attributes = dict(zip(strs, all_values))

        for key, val in six.viewitems(attributes):
            experiment_run.log_attribute(key, val)
            with pytest.raises(ValueError):
                experiment_run.log_attribute(key, val)

        # try it backwards, too
        for key, val in reversed(list(six.viewitems(attributes))):
            with pytest.raises(ValueError):
                experiment_run.log_attribute(key, val)

    def test_atomic(self, experiment_run, strs, all_values):
        """batch completely fails even if only a single key conflicts"""
        attributes = dict(zip(strs, all_values))
        first_attribute = (strs[0], all_values[0])

        experiment_run.log_attribute(*first_attribute)

        with pytest.raises(ValueError):
            experiment_run.log_attributes(attributes)

        assert experiment_run.get_attributes() == dict([first_attribute])

    def test_nonstring_key_error(self, experiment_run, scalar_values):
        scalar_values = (value for value in scalar_values if not isinstance(value, str))  # rm str
        attributes = dict(zip(scalar_values, scalar_values))

        for key, val in six.viewitems(attributes):
            with pytest.raises(TypeError):
                experiment_run.log_attribute(key, val)


@pytest.mark.skipif(
    importer.maybe_dependency("scipy") is None,
    reason="scipy is not installed",
)
class TestComplexAttributes:
    def test_creation(self, client, strs):
        key = strs[0]
        attr = data_types.DiscreteHistogram(
            buckets=["yes", "no"],
            data=[10, 20],
        )

        experiment_run = client.create_experiment_run(
            attrs={key: attr},
        )
        assert experiment_run.get_attribute(key) == attr

    def test_single(self, experiment_run, strs):
        key = strs[0]
        attr = data_types.Line(
            x=[1, 2, 3],
            y=[1, 4, 9],
        )

        experiment_run.log_attribute(key, attr)
        assert experiment_run.get_attribute(key) == attr

    def test_batch(self, experiment_run, strs):
        key1, key2, key3 = strs[:3]
        attr1 = data_types.Table(
            data=[[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        attr2 = data_types.ConfusionMatrix(
            value=[[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            labels=["a", "b", "c"],
        )
        attr3 = {"a": 1}

        experiment_run.log_attribute(key1, attr1)
        assert experiment_run.get_attribute(key1) == attr1
        experiment_run.log_attribute(key2, attr2)
        assert experiment_run.get_attribute(key2) == attr2
        experiment_run.log_attribute(key3, attr3)

        assert experiment_run.get_attributes() == {
            key1: attr1,
            key2: attr2,
            key3: attr3,
        }
