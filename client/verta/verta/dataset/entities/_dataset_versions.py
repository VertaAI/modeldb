# -*- coding: utf-8 -*-

import copy

from verta._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService

from verta._internal_utils import _utils
from . import _dataset_version


class DatasetVersions(_utils.LazyList):
    r"""
    ``list``-like object containing :class:`~verta.dataset.entities.DatasetVersion`\ s.

    This class provides functionality for filtering and sorting its contents.

    There should not be a need to instantiate this class directly; please use
    :class:`Dataset.versions <verta.dataset.entities.Dataset>`.

    Examples
    --------
    .. code-block:: python

        versions = dataset.versions.find("tags ~= normalized")
        version = versions.sort("time_created", descending=True)[0]  # most recent

    """
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'dataset_id',
        'version',
        'tags',
        'time_logged',
    }

    def __init__(self, conn, conf):
        super(DatasetVersions, self).__init__(
            conn, conf,
            _DatasetVersionService.FindDatasetVersions(),
        )

    def __repr__(self):
        return "<DatasetVersions containing {} dataset versions>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                 "/api/v1/modeldb/dataset-version/findDatasetVersions",
                                                 body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.dataset_versions, response.total_records

    def _create_element(self, msg):
        return _dataset_version.DatasetVersion(self._conn, self._conf, msg)

    def with_dataset(self, dataset):
        """
        Returns versions of the specified dataset.

        Parameters
        ----------
        dataset : :class:`~verta.dataset.entities.Dataset` or None
            Dataset. If ``None``, returns versions across all datasets.

        Returns
        -------
        :class:`DatasetVersions`
            Filtered dataset versions.

        """
        new_list = copy.deepcopy(self)
        if dataset:
            new_list._msg.dataset_id = dataset.id
        else:
            new_list._msg.dataset_id = ''
        return new_list
