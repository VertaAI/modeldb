# -*- coding: utf-8 -*-

from __future__ import print_function

import ast
import copy
import re
import warnings

import pandas as pd

from .experimentrun import ExperimentRun

from .._protos.public.modeldb import CommonService_pb2 as _CommonService
from .._protos.public.modeldb import ExperimentRunService_pb2 as _ExperimentRunService

from ..external import six

from .._internal_utils import (
    _utils,
)


class ExperimentRuns(_utils.LazyList):
    r"""
    ``list``-like object representing a collection of machine learning Experiment Runs.

    This class provides functionality for filtering and sorting its contents.

    There should not be a need to instantiate this class directly; please use other classes'
    attributes to access Experiment Runs.

    Examples
    --------
    .. code-block:: python

        runs = expt.find("hyperparameters.hidden_size == 256")
        len(runs)
        # 12
        runs += expt.find("hyperparameters.hidden_size == 512")
        len(runs)
        # 24
        runs = runs.find("metrics.accuracy >= .8")
        len(runs)
        # 5
        runs[0].get_metric("accuracy")
        # 0.8921755939794525

    """
    _OP_MAP = {'==': _CommonService.OperatorEnum.EQ,
               '!=': _CommonService.OperatorEnum.NE,
               '>':  _CommonService.OperatorEnum.GT,
               '>=': _CommonService.OperatorEnum.GTE,
               '<':  _CommonService.OperatorEnum.LT,
               '<=': _CommonService.OperatorEnum.LTE}
    _OP_PATTERN = re.compile(r"({})".format('|'.join(sorted(six.viewkeys(_OP_MAP), key=lambda s: len(s), reverse=True))))

    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id', 'project_id', 'experiment_id',
        'name',
        'date_created',
        'attributes', 'hyperparameters', 'metrics',
    }

    def __init__(self, conn, conf):
        super(ExperimentRuns, self).__init__(
            conn, conf,
            _ExperimentRunService.FindExperimentRuns(),
            "{}://{}/api/v1/modeldb/experiment-run/findExperimentRuns",
            "POST",
        )

    def __repr__(self):
        return "<ExperimentRuns containing {} runs>".format(self.__len__())

    def _get_records(self, response_msg):
        return response_msg.experiment_runs

    def _create_element(self, msg):
        return ExperimentRun(self._conn, self._conf, msg)

    def as_dataframe(self):
        data = []
        columns = set()
        for run in self:
            run_data = {'id': run.id}

            run_data.update({'hpp.'+k: v for k, v in run.get_metrics().items()})
            columns = columns.union(set(['hpp.'+k for k in run.get_metrics().keys()]))

            run_data.update({'metric.'+k: v for k, v in run.get_metrics().items()})
            columns = columns.union(set(['metric.'+k for k in run.get_metrics().keys()]))

            data.append(run_data)

        return pd.DataFrame(data, columns=['id'] + sorted(list(columns)))


    def find(self, where, ret_all_info=False):
        """
        Gets the Experiment Runs from this collection that match predicates `where`.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A predicate in `where` is a string containing a simple boolean expression consisting of:

            - a dot-delimited Experiment Run property such as ``metrics.accuracy``
            - a Python boolean operator such as ``>=``
            - a literal value such as ``.8``

        Parameters
        ----------
        where : str or list of str
            Predicates specifying Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.find(["hyperparameters.hidden_size == 256",
                       "metrics.accuracy >= .8"])
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        new_runs = copy.deepcopy(self)

        if isinstance(where, six.string_types):
            where = [where]
        for predicate in where:
            # split predicate
            try:
                key, operator, value = map(lambda token: token.strip(), self._OP_PATTERN.split(predicate, maxsplit=1))
            except ValueError:
                six.raise_from(ValueError("predicate `{}` must be a two-operand comparison".format(predicate)),
                               None)

            if key.split('.')[0] not in self._VALID_QUERY_KEYS:
                raise ValueError("key `{}` is not a valid key for querying;"
                                 " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

            # cast operator into protobuf enum variant
            operator = self._OP_MAP[operator]

            try:
                value = float(value)
            except ValueError:  # not a number
                # parse value
                try:
                    expr_node = ast.parse(value, mode='eval')
                except SyntaxError:
                    e = ValueError("value `{}` must be a number or string literal".format(value))
                    six.raise_from(e, None)
                value_node = expr_node.body
                if type(value_node) is ast.Str:
                    value = value_node.s
                elif type(value_node) is ast.Compare:
                    e = ValueError("predicate `{}` must be a two-operand comparison".format(predicate))
                    six.raise_from(e, None)
                else:
                    e = ValueError("value `{}` must be a number or string literal".format(value))
                    six.raise_from(e, None)

            new_runs._msg.predicates.append(  # pylint: disable=no-member
                _CommonService.KeyValueQuery(
                    key=key, value=_utils.python_to_val_proto(value),
                    operator=operator,
                )
            )

        return new_runs

    def sort(self, key, descending=False, ret_all_info=False):
        """
        Sorts the Experiment Runs from this collection by `key`.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as
        ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        descending : bool, default False
            Order in which to return sorted Experiment Runs.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.sort("metrics.accuracy")
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        new_runs = copy.deepcopy(self)

        new_runs._msg.sort_key = key
        new_runs._msg.ascending = not descending

        return new_runs

    def top_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` highest `key`\ s.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as
        ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        k : int
            Number of Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.top_k("metrics.accuracy", 3)
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        # apply sort to new Runs
        new_runs = copy.deepcopy(self)
        new_runs._msg.sort_key = key
        new_runs._msg.ascending = False

        # copy msg to avoid mutating `new_runs`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(new_runs._msg)
        msg.page_limit = k
        msg.page_number = 1

        response_msg = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in response_msg.experiment_runs)

        return new_runs

    def bottom_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` lowest `key`\ s.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        k : int
            Number of Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.bottom_k("metrics.loss", 3)
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        # apply sort to new Runs
        new_runs = copy.deepcopy(self)
        new_runs._msg.sort_key = key
        new_runs._msg.ascending = True

        # copy msg to avoid mutating `new_runs`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(new_runs._msg)
        msg.page_limit = k
        msg.page_number = 1

        response_msg = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in response_msg.experiment_runs)

        return new_runs
