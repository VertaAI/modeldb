# -*- coding: utf-8 -*-

from __future__ import print_function

import copy
import warnings

from .experimentrun import ExperimentRun

from .._protos.public.modeldb import ExperimentRunService_pb2 as _ExperimentRunService

from ..external import six

from .._internal_utils import (
    _utils,
    importer,
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
        runs = runs.find("metrics.accuracy >= .8")
        len(runs)
        # 5
        runs[0].get_metric("accuracy")
        # 0.8921755939794525

    """
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id', 'project_id', 'experiment_id',
        'name',
        'date_created', 'date_updated',
        'attributes', 'hyperparameters', 'metrics',
    }

    def __init__(self, conn, conf):
        super(ExperimentRuns, self).__init__(
            conn, conf,
            _ExperimentRunService.FindExperimentRuns(),
        )

    def __repr__(self):
        return "<ExperimentRuns containing {} runs>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                "/api/v1/modeldb/experiment-run/findExperimentRuns",
                                                body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.experiment_runs, response.total_records

    def _create_element(self, msg):
        return ExperimentRun(self._conn, self._conf, msg)

    def as_dataframe(self):
        """
        Returns this collection of Experiment Runs as a table.

        Returns
        -------
        :class:`pandas.DataFrame`

        """
        pd = importer.maybe_dependency("pandas")
        if pd is None:
            e = ImportError("pandas is not installed; try `pip install pandas`")
            six.raise_from(e, None)

        ids = []
        data = []
        for run in self:
            run_data = {}
            run_data.update({'hpp.'+k: v for k, v in run.get_hyperparameters().items()})
            run_data.update({'metric.'+k: v for k, v in run.get_metrics().items()})

            ids.append(run.id)
            data.append(run_data)

        columns = set()
        for run_data in data:
            columns.update(run_data.keys())

        return pd.DataFrame(data, index=ids, columns=sorted(list(columns)))

    def with_project(self, proj=None):
        new_list = copy.deepcopy(self)
        if proj:
            new_list._msg.project_id = proj.id
        else:
            new_list._msg.project_id = ''
        return new_list

    def with_experiment(self, expt=None):
        new_list = copy.deepcopy(self)
        if expt:
            new_list._msg.experiment_id = expt.id
        else:
            new_list._msg.experiment_id = ''
        return new_list

    def top_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` highest `key`\ s.

        .. versionchanged:: 0.14.12
           The `ret_all_info` parameter was removed.

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

        records, _ = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in records)

        return new_runs

    def bottom_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` lowest `key`\ s.

        .. versionchanged:: 0.14.12
           The `ret_all_info` parameter was removed.

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

        records, _ = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in records)

        return new_runs
