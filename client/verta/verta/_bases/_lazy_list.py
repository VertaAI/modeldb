# -*- coding: utf-8 -*-

import abc
import copy
import re

from verta.external import six
from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._internal_utils import _utils


@six.add_metaclass(abc.ABCMeta)
class _PaginatedIterable(object):
    """
    Iterable base class for Verta entites.

    Supports:

    * indexing
    * paginated iteration.

    Examples
    --------
    .. code-block:: python

        expt.expt_runs
        # <ExperimentRuns containing 3 runs>

        expt.expt_runs[0]
        # name: Run 186381627658178606302
        # url: https://app.verta.ai/...
        # ...

        for run in expt.expt_runs:
            do_something(run)

    """

    # number of items to fetch per back end call in __iter__()
    _ITER_PAGE_LIMIT = 100

    # TODO: set _msg in an abstract class property, so subclasses don't have
    #       to awkwardly override __init__() with different params
    def __init__(self, conn, conf, msg):
        self._conn = conn
        self._conf = conf
        self._msg = msg  # protobuf msg used to make back end calls
        self.limit = self._ITER_PAGE_LIMIT

    def __getitem__(self, index):
        if isinstance(index, int):
            # copy msg to avoid mutating `self`'s state
            msg = self._msg.__class__()
            msg.CopyFrom(self._msg)
            msg = self._set_page_limit(msg, 1)
            if index >= 0:
                # convert zero-based indexing into page number
                msg = self._set_page_number(msg, index + 1)
            else:
                # reverse page order to index from end
                msg.ascending = not msg.ascending  # pylint: disable=no-member
                msg = self._set_page_number(msg, abs(index))

            records, total_records = self._call_back_end(msg)
            if not records and self._page_number(msg) > total_records:
                raise IndexError("index out of range")

            return self._create_element(records[0])
        else:
            raise TypeError("index must be integer, not {}".format(type(index)))

    def __iter__(self):
        # copy msg to avoid mutating `self`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(self._msg)
        self._set_page_limit(msg, self.limit)
        # page number will be incremented as soon as we enter the loop
        self._set_page_number(msg, 0)

        seen_ids = set()
        total_records = float("inf")
        page_number = self._page_number(msg)
        while self._page_limit(msg) * page_number < total_records:
            page_number += 1
            self._set_page_number(msg, page_number)

            records, total_records = self._call_back_end(msg)
            for rec in records:
                # skip if we've seen the ID before
                if rec.id in seen_ids:
                    continue
                else:
                    seen_ids.add(rec.id)

                yield self._create_element(rec)

    def __len__(self):
        # copy msg to avoid mutating `self`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(self._msg)
        # minimal request just to get total_records
        self._set_page_limit(msg, 1)
        self._set_page_number(msg, 1)

        _, total_records = self._call_back_end(msg)

        return total_records

    @abc.abstractmethod
    def _call_back_end(self, msg):
        """Find the request in the backend and returns (elements, total count)."""
        raise NotImplementedError

    @abc.abstractmethod
    def _create_element(self, msg):
        """Instantiate element to return to user."""
        raise NotImplementedError

    def set_page_limit(self, limit):
        """
        Sets the number of entities to fetch per backend call during iteration.

        By default, each call fetches a batch of 100 entities, but lowering
        this value may be useful for substantially larger responses.

        Parameters
        ----------
        limit : int
            Number of entities to fetch per call.

        Examples
        --------
        .. code-block:: python

            runs = proj.expt_runs
            runs.set_page_limit(10)
            for run in runs:  # fetches 10 runs per backend call
                print(run.get_metric("accuracy"))

        """
        if not isinstance(limit, six.integer_types):
            raise TypeError("`limit` must be int, not {}".format(type(limit)))

        self.limit = limit

    def _set_page_limit(self, msg, param):
        msg.page_limit = param
        return msg

    def _set_page_number(self, msg, param):
        msg.page_number = param
        return msg

    def _page_limit(self, msg):
        return msg.page_limit

    def _page_number(self, msg):
        return msg.page_number


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
