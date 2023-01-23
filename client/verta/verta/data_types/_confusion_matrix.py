# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class ConfusionMatrix(_VertaDataType):
    """
    Representation of a confusion matrix.

    Parameters
    ----------
    value : list of list of float
        Cell values.
    labels : list of str
        Row/column labels.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import ConfusionMatrix
        data = ConfusionMatrix(
            value=[
                [6, 1],
                [2, 3],
            ],
            labels=["spam", "not spam"],
        )
        run.log_attribute("spam_confusion_matrix", data)

    """

    _TYPE_NAME = "confusionMatrix"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value, labels):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if len(value) != len(value[0]):
            raise ValueError("rows and columns in `value` must have the same length")
        if len(labels) != len(value[0]):
            raise ValueError("length of `columns` must equal length of rows in `value`")
        if not arg_handler.contains_only_numbers(value):
            raise TypeError("`value` must contain only numbers")

        self._value = value
        self._labels = labels

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "value": self._value,
                "labels": self._labels,
            }
        )

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(
            value=data["value"],
            labels=data["labels"],
        )

    def diff(self, other):
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        raise NotImplementedError  # TODO
