import pytest

import verta.configuration


class TestInternalFunctions:
    def test_value_to_msg(self):
        fn = verta.configuration.Hyperparameters._value_to_msg

        assert fn(0)
        assert fn(0.3)
        assert fn("coconut")

    def test_hyperparameter_to_msg(self):
        fn = verta.configuration.Hyperparameters._hyperparameter_to_msg

        assert fn("banana", 0)
        assert fn("banana", 0.3)
        assert fn("banana", "foo")

    def test_hyperparamater_range_to_msg(self):
        fn = verta.configuration.Hyperparameters._hyperparameter_range_to_msg

        assert fn("banana", (0, 12, 3))
        with pytest.raises(TypeError):
            fn("banana", 0)
        with pytest.raises(ValueError):
            fn("banana", (0, 12))

    def test_hyperparameter_set_to_msg(self):
        fn = verta.configuration.Hyperparameters._hyperparameter_set_to_msg

        assert fn("banana", list(range(0, 12, 3)))
        with pytest.raises(TypeError):
            fn("banana", 0)

class TestHyperparameters:
    HYPERPARAMETERS = {'banana': "foo"}
    HYPERPARAMETER_RANGES = {'coconut': (0, 12, 3)}
    HYPERPARAMETER_SETS = {'durian': list(range(0, 12, 3))}

    def test_hyperparameters(self):
        config = verta.configuration.Hyperparameters(
            hyperparameters=self.HYPERPARAMETERS,
        )

        name, value = list(self.HYPERPARAMETERS.items())[0]

        hyperparam_msg = config._msg.hyperparameters[0]
        assert hyperparam_msg.name == name
        value_msg = hyperparam_msg.value
        assert getattr(value_msg, value_msg.WhichOneof('value')) == value

    def test_hyperparamater_ranges(self):
        config = verta.configuration.Hyperparameters(
            hyperparameter_ranges=self.HYPERPARAMETER_RANGES,
        )

        name, (begin, end, step) = list(self.HYPERPARAMETER_RANGES.items())[0]

        hyperparam_msg = config._msg.hyperparameter_set[0]
        assert hyperparam_msg.name == name
        begin_msg = hyperparam_msg.continuous.interval_begin
        assert getattr(begin_msg, begin_msg.WhichOneof('value')) == begin
        end_msg = hyperparam_msg.continuous.interval_end
        assert getattr(end_msg, end_msg.WhichOneof('value')) == end
        step_msg = hyperparam_msg.continuous.interval_step
        assert getattr(step_msg, step_msg.WhichOneof('value')) == step

    def test_hyperparameter_sets(self):
        config = verta.configuration.Hyperparameters(
            hyperparameter_sets=self.HYPERPARAMETER_SETS,
        )

        name, sequence = list(self.HYPERPARAMETER_SETS.items())[0]

        hyperparam_msg = config._msg.hyperparameter_set[0]
        assert hyperparam_msg.name == name
        for value_msg, value in zip(hyperparam_msg.discrete.values, sequence):
            assert getattr(value_msg, value_msg.WhichOneof('value')) == value

    def test_commit(self, commit):
        config = verta.configuration.Hyperparameters(
            hyperparameters=self.HYPERPARAMETERS,
            hyperparameter_ranges=self.HYPERPARAMETER_RANGES,
            hyperparameter_sets=self.HYPERPARAMETER_SETS,
        )

        commit.update('config', config)
        commit.save(message="banana")
        assert commit.get('config')

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        config = verta.configuration.Hyperparameters(
            hyperparameters={
                'a': 1, 'b': 1,
            },
            hyperparameter_ranges={
                'c': (1, 5, 1), 'd': (1, 5, 1),
            },
            hyperparameter_sets={
                'e': [1, 2], 'f': [1, 2],
            },
        )

        assert config.__repr__()
