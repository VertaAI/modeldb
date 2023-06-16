# -*- coding: utf-8 -*-

import abc
import copy
import re

from verta._vendored import six
from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._internal_utils import _utils

from ._paginated_iterable import _PaginatedIterable


@six.add_metaclass(abc.ABCMeta)
class _LazyList(_PaginatedIterable):
    _OP_MAP = {
        "~=": _CommonCommonService.OperatorEnum.CONTAIN,
        "==": _CommonCommonService.OperatorEnum.EQ,
        "!=": _CommonCommonService.OperatorEnum.NE,
        ">": _CommonCommonService.OperatorEnum.GT,
        ">=": _CommonCommonService.OperatorEnum.GTE,
        "<": _CommonCommonService.OperatorEnum.LT,
        "<=": _CommonCommonService.OperatorEnum.LTE,
    }
    _OP_PATTERN = re.compile(
        r" ({}) ".format("|".join(sorted(six.viewkeys(_OP_MAP), key=len, reverse=True)))
    )

    # keys that yield predictable, sensible results
    # TODO: make this attr an abstract static method
    _VALID_QUERY_KEYS = dict()  # NOTE: must be overridden by subclasses

    def find(self, *args):
        """
        Gets the results from this collection that match input predicates.

        A predicate is a string containing a simple boolean expression consisting of:

            - a dot-delimited property such as ``metrics.accuracy``
            - a Python boolean operator such as ``>=``
            - a literal value such as ``.8``

        Parameters
        ----------
        *args : strs
            Predicates specifying results to get.

        Returns
        -------
        The same type of object given in the input.

        Examples
        --------
        .. code-block:: python

            runs.find("hyperparameters.hidden_size == 256",
                       "metrics.accuracy >= .8")
            # <ExperimentRuns containing 3 runs>
            # alternatively:
            runs.find(["hyperparameters.hidden_size == 256",
                       "metrics.accuracy >= .8"])
            # <ExperimentRuns containing 3 runs>

        """
        if len(args) == 1 and isinstance(args[0], (list, tuple)):
            # to keep backward compatibility, in case user pass in a list or tuple
            return self.find(*args[0])
        elif not all(isinstance(predicate, six.string_types) for predicate in args):
            raise TypeError("predicates must all be strings")

        new_list = copy.deepcopy(self)
        for predicate in args:
            # split predicate
            try:
                key, operator, value = map(
                    lambda token: token.strip(),
                    self._OP_PATTERN.split(predicate, maxsplit=1),
                )
            except ValueError:
                six.raise_from(
                    ValueError(
                        "predicate `{}` must be a two-operand comparison".format(
                            predicate
                        )
                    ),
                    None,
                )

            if key.split(".")[0] not in self._VALID_QUERY_KEYS:
                raise ValueError(
                    "key `{}` is not a valid key for querying;"
                    " currently supported keys are: {}".format(
                        key, self._VALID_QUERY_KEYS
                    )
                )

            # cast operator into protobuf enum variant
            operator = self._OP_MAP[operator]

            try:
                value = float(value)
            except ValueError:  # not a number, so process as string
                # maintain old behavior where input would be wrapped in quotes
                if (value.startswith("'") and value.endswith("'")) or (
                    value.startswith('"') and value.endswith('"')
                ):
                    value = value[1:-1]

            new_list._msg.predicates.append(  # pylint: disable=no-member
                _CommonCommonService.KeyValueQuery(
                    key=key,
                    value=_utils.python_to_val_proto(value),
                    operator=operator,
                )
            )

        return new_list

    def sort(self, key, descending=False):
        """
        Sorts the results from this collection by `key`.

        A `key` is a string containing a dot-delimited property such as
        ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited property.
        descending : bool, default False
            Order in which to return sorted results.

        Returns
        -------
        The same type of object given in the input.

        Examples
        --------
        .. code-block:: python

            runs.sort("metrics.accuracy")
            # <ExperimentRuns containing 3 runs>

        """
        if key.split(".")[0] not in self._VALID_QUERY_KEYS:
            raise ValueError(
                "key `{}` is not a valid key for querying;"
                " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS)
            )

        new_list = copy.deepcopy(self)

        new_list._msg.sort_key = key
        new_list._msg.ascending = not descending

        return new_list
