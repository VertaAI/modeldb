# -*- coding: utf-8 -*-

import copy

from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService

from .._internal_utils import _utils
from . import dataset


class Datasets(_utils.LazyList):
    r"""
    ``list``-like object containing :class:`~verta._dataset_versioning.dataset.Dataset`\ s.

    This class provides functionality for filtering and sorting its contents.

    There should not be a need to instantiate this class directly; please use
    :class:`Client.datasets <verta.client.Client>`.

    Examples
    --------
    .. code-block:: python

        datasets = client.datasets.find("tags ~= census")
        dataset = datasets.sort("time_created", descending=True)[0]  # most recent

    """
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'name',
        'tags',
        'time_created',
    }

    def __init__(self, conn, conf):
        super(Datasets, self).__init__(
            conn, conf,
            _DatasetService.FindDatasets(),
        )

    def __repr__(self):
        return "<Datasets containing {} datasets>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                 "/api/v1/modeldb/dataset/findDatasets",
                                                 body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.datasets, response.total_records

    def _create_element(self, msg):
        return dataset.Dataset(self._conn, self._conf, msg)

    def with_workspace(self, workspace_name=None):
        """
        Returns datasets in the specified workspace.

        Parameters
        ----------
        workspace_name : str or None, default None
            Workspace name. If ``None``, uses personal workspace.

        Returns
        -------
        :class:`Datasets`
            Filtered datasets.

        """
        new_list = copy.deepcopy(self)
        new_list._msg.workspace_name = workspace_name
        return new_list

    def with_ids(self, ids):
        """
        Returns datasets with the specified IDs.

        Parameters
        ----------
        ids : list of str
            Dataset IDs.

        Returns
        -------
        :class:`Datasets`
            Filtered datasets.

        """
        new_list = copy.deepcopy(self)
        del new_list._msg.dataset_ids[:]
        if ids:
            new_list._msg.dataset_ids.extend(ids)
        return new_list
