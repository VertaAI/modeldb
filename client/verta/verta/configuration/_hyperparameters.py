# -*- coding: utf-8 -*-

from __future__ import print_function

from ..external import six

from .._protos.public.modeldb.versioning import Config_pb2 as _ConfigService

from . import _configuration


class Hyperparameters(_configuration._Configuration):
    """
    Captures hyperparameters.

    Parameters
    ----------
    hyperparameters : dict of `name` to `value`
        Hyperparameter names to individual values.
    hyperparameter_ranges : dict of `name` to tuple of (`start`, `stop`, `step`)
        Hyperparameter names to a specified range of values.
    hyperparameter_sets : dict of `name` to list of `values`
        Hyperparameter names to sets of specific values.

    Examples
    --------
    .. code-block:: python

        from verta.configuration import Hyperparameters
        config1 = Hyperparameters(hyperparameters={
            'C': 1e-4,
            'penalty': 'l2',
        })
        config2 = Hyperparameters(hyperparameter_ranges={
            'C': (0, 1, 1e-2),
        })
        config3 = Hyperparameters(hyperparameter_sets={
            'penalty': ['l1', 'l2'],
        })

    """
    def __init__(self, hyperparameters=None, hyperparameter_ranges=None, hyperparameter_sets=None):
        super(Hyperparameters, self).__init__()

        if hyperparameters is not None:
            self._msg.hyperparameters.extend(
                self._hyperparameter_to_msg(name, value)
                for name, value
                in six.viewitems(hyperparameters)
            )

        if hyperparameter_ranges is not None:
            self._msg.hyperparameter_set.extend(
                self._hyperparameter_range_to_msg(name, range_)
                for name, range_
                in six.viewitems(hyperparameter_ranges)
            )

        if hyperparameter_sets is not None:
            self._msg.hyperparameter_set.extend(
                self._hyperparameter_set_to_msg(name, values)
                for name, values
                in six.viewitems(hyperparameter_sets)
            )

    @staticmethod
    def _value_to_msg(value):
        """
        Converts a hyperparameter value into a protobuf message.

        Parameters
        ----------
        value : int or float or str

        Returns
        -------
        msg : HyperparameterValuesConfigBlob

        """
        msg = _ConfigService.HyperparameterValuesConfigBlob()

        if isinstance(value, int):
            msg.int_value = value
        elif isinstance(value, float):
            msg.float_value = value
        elif isinstance(value, six.string_types):
            msg.string_value = value
        else:
            raise TypeError("value {} must be either int, float, or str,"
                            " not {}".format(value, type(value)))

        return msg

    @classmethod
    def _hyperparameter_to_msg(cls, name, value):
        msg = _ConfigService.HyperparameterConfigBlob()

        msg.name = name

        msg.value.CopyFrom(cls._value_to_msg(value))

        return msg

    @classmethod
    def _hyperparameter_range_to_msg(cls, name, range_):
        if len(range_) != 3:
            raise ValueError("range {} must be a 3-tuple of int in the form"
                             " (start, stop, step)".format(range_))

        msg = _ConfigService.HyperparameterSetConfigBlob()

        msg.name = name

        msg.continuous.interval_begin.CopyFrom(cls._value_to_msg(range_[0]))
        msg.continuous.interval_end.CopyFrom(cls._value_to_msg(range_[1]))
        msg.continuous.interval_step.CopyFrom(cls._value_to_msg(range_[2]))

        return msg

    @classmethod
    def _hyperparameter_set_to_msg(cls, name, values):
        msg = _ConfigService.HyperparameterSetConfigBlob()

        msg.name = name

        msg.discrete.values.extend(map(cls._value_to_msg, values))

        return msg
