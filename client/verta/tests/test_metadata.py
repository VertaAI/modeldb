import six

import string

import verta
from verta._internal_utils import _utils

import pytest
from . import utils


class TestTags:
    def test_log_single(self, experiment_run, strs):
        for tag in strs:
            experiment_run.log_tag(tag)

        assert set(experiment_run.get_tags()) == set(strs)

    def test_log_batch(self, experiment_run, strs):
        experiment_run.log_tags(strs)

        assert set(experiment_run.get_tags()) == set(strs)

    def test_ignore_duplicates(self, experiment_run, strs):
        """duplicate tags do not raise an error, and instead are ignored"""
        experiment_run.log_tags(strs*2)

        assert set(experiment_run.get_tags()) == set(strs)

        for tag in strs:
            experiment_run.log_tag(tag)

        assert set(experiment_run.get_tags()) == set(strs)

        experiment_run.log_tags(strs)

        assert set(experiment_run.get_tags()) == set(strs)

    def test_log_nonstring_error(self, experiment_run, all_values):
        for value in (value for value in all_values if not isinstance(value, str)):
            with pytest.raises(TypeError):
                experiment_run.log_tag(value)


class TestHyperparameters:
    def test_keys(self, experiment_run):
        keys = (c for c in string.printable if c not in _utils._VALID_FLAT_KEY_CHARS)
        for key in keys:
            with pytest.raises(ValueError):
                experiment_run.log_hyperparameter(key, 'key test')

    def test_single(self, experiment_run, strs, scalar_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        hyperparameters = dict(zip(strs, scalar_values))

        for key, val in six.viewitems(hyperparameters):
            experiment_run.log_hyperparameter(key, val)

        with pytest.raises(KeyError):
            experiment_run.get_hyperparameter(holdout)

        for key, val in six.viewitems(hyperparameters):
            assert experiment_run.get_hyperparameter(key) == val

        assert experiment_run.get_hyperparameters() == hyperparameters

    def test_batch(self, experiment_run, strs, scalar_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        hyperparameters = dict(zip(strs, scalar_values))

        experiment_run.log_hyperparameters(hyperparameters)

        with pytest.raises(KeyError):
            experiment_run.get_hyperparameter(holdout)

        for key, val in six.viewitems(hyperparameters):
            assert experiment_run.get_hyperparameter(key) == val

        assert experiment_run.get_hyperparameters() == hyperparameters

    def test_conflict(self, experiment_run, strs, scalar_values):
        hyperparameters = dict(zip(strs, scalar_values))

        for key, val in six.viewitems(hyperparameters):
            experiment_run.log_hyperparameter(key, val)
            experiment_run.log_hyperparameter(key, val, overwrite=True)
            with pytest.raises(ValueError):
                experiment_run.log_hyperparameter(key, val)

        # try it backwards, too
        for key, val in reversed(list(six.viewitems(hyperparameters))):
            with pytest.raises(ValueError):
                experiment_run.log_hyperparameter(key, val)

    def test_atomic(self, experiment_run, strs, scalar_values):
        """batch completely fails even if only a single key conflicts"""
        hyperparameters = dict(zip(strs, scalar_values))
        first_hyperparameter = (strs[0], scalar_values[0])

        experiment_run.log_hyperparameter(*first_hyperparameter)

        with pytest.raises(ValueError):
            experiment_run.log_hyperparameters(hyperparameters)

        assert experiment_run.get_hyperparameters() == dict([first_hyperparameter])

    def test_collection_error(self, experiment_run, strs, collection_values):
        """do not permit logging lists or dicts"""
        hyperparameters = dict(zip(strs, collection_values))

        # single
        for key, val in six.viewitems(hyperparameters):
            with pytest.raises(TypeError):
                experiment_run.log_hyperparameter(key, val)

        # batch
        with pytest.raises(TypeError):
            experiment_run.log_hyperparameters(hyperparameters)


class TestAttributes:
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


class TestMetrics:
    def test_keys(self, experiment_run):
        keys = (c for c in string.printable if c not in _utils._VALID_FLAT_KEY_CHARS)
        for key in keys:
            with pytest.raises(ValueError):
                experiment_run.log_metric(key, 'key test')

    def test_single(self, experiment_run, strs, scalar_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        metrics = dict(zip(strs, scalar_values))

        for key, val in six.viewitems(metrics):
            experiment_run.log_metric(key, val)

        with pytest.raises(KeyError):
            experiment_run.get_metric(holdout)

        for key, val in six.viewitems(metrics):
            assert experiment_run.get_metric(key) == val

        assert experiment_run.get_metrics() == metrics

    def test_batch(self, experiment_run, strs, scalar_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        metrics = dict(zip(strs, scalar_values))

        experiment_run.log_metrics(metrics)
        experiment_run.log_metrics(metrics, overwrite=True)

        with pytest.raises(KeyError):
            experiment_run.get_metric(holdout)

        for key, val in six.viewitems(metrics):
            assert experiment_run.get_metric(key) == val

        assert experiment_run.get_metrics() == metrics

    def test_conflict(self, experiment_run, strs, scalar_values):
        metrics = dict(zip(strs, scalar_values))

        for key, val in six.viewitems(metrics):
            experiment_run.log_metric(key, val)
            experiment_run.log_metric(key, val, overwrite=True)
            with pytest.raises(ValueError):
                experiment_run.log_metric(key, val)

        # try it backwards, too
        for key, val in reversed(list(six.viewitems(metrics))):
            with pytest.raises(ValueError):
                experiment_run.log_metric(key, val)

    def test_atomic(self, experiment_run, strs, scalar_values):
        """batch completely fails even if only a single key conflicts"""
        metrics = dict(zip(strs, scalar_values))
        first_metric = (strs[0], scalar_values[0])

        experiment_run.log_metric(*first_metric)

        with pytest.raises(ValueError):
            experiment_run.log_metrics(metrics)

        assert experiment_run.get_metrics() == dict([first_metric])

    def test_collection_error(self, experiment_run, strs, collection_values):
        """do not permit logging lists or dicts"""
        metrics = dict(zip(strs, collection_values))

        # single
        for key, val in six.viewitems(metrics):
            with pytest.raises(TypeError):
                experiment_run.log_metric(key, val)

        # batch
        with pytest.raises(TypeError):
            experiment_run.log_metrics(metrics)


class TestObservations:
    def test_keys(self, experiment_run):
        keys = (c for c in string.printable if c not in _utils._VALID_FLAT_KEY_CHARS)
        for key in keys:
            with pytest.raises(ValueError):
                experiment_run.log_observation(key, 'key test')

    def test_single(self, experiment_run, strs, scalar_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        observations = {
            key: [scalar_value]*3
            for key, scalar_value in zip(strs, scalar_values)
        }

        for key, vals in six.viewitems(observations):
            for val in vals:
                experiment_run.log_observation(key, val)

        with pytest.raises(KeyError):
            experiment_run.get_observation(holdout)

        for key, val in six.viewitems(observations):
            assert [obs_tuple[0] for obs_tuple in experiment_run.get_observation(key)] == val

        assert {key: [obs_tuple[0] for obs_tuple in obs_seq]
                for key, obs_seq in experiment_run.get_observations().items()} == observations

    def test_collection_error(self, experiment_run, strs, collection_values):
        """do not permit logging lists or dicts"""
        observations = {
            key: [collection_value]*3
            for key, collection_value in zip(strs, collection_values)
        }

        for key, vals in six.viewitems(observations):
            for val in vals:
                with pytest.raises(TypeError):
                    experiment_run.log_observation(key, val)

    def test_epoch_num(self, experiment_run, strs, ints):
        key = strs[0]
        values = iter(set(ints))  # unique integers
        epoch_num = 3

        # backend auto start at 0
        value1 = next(values)
        experiment_run.log_observation(key, value1)

        # manually pass `epoch_num`
        value2 = next(values)
        experiment_run.log_observation(key, value2, epoch_num=epoch_num)

        # if not passed, backend auto-increments
        value3 = next(values)
        experiment_run.log_observation(key, value3)

        # accept duplicate `epoch_num`
        value4 = next(values)
        experiment_run.log_observation(key, value4, epoch_num=epoch_num)

        value_to_epoch = {
            obs_tuple[0]: obs_tuple[2]
            for obs_tuple
            in experiment_run.get_observation(key)
        }
        assert value_to_epoch[value1] == 0  # start at 0
        assert value_to_epoch[value2] == epoch_num  # manual
        assert value_to_epoch[value3] == epoch_num + 1  # auto-increment
        assert value_to_epoch[value4] == epoch_num  # duplicate
