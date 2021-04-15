# -*- coding: utf-8 -*-

import abc

from verta.external import six

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.common import comparison as comparison_module
from .. import utils


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _Alerter(object):
    _TYPE = _AlertService.AlerterTypeEnum.UNKNOWN

    def __repr__(self):
        return "<{} alert>".format(
            _AlertService.AlerterTypeEnum.AlerterType.Name(self._TYPE).lower()
        )

    @abc.abstractmethod
    def _as_proto(self):
        raise NotImplementedError


class FixedAlerter(_Alerter):
    """
    A alerter with a fixed numerical threshold.

    .. note::

        This alerter is only intended to work with summary samples of the
        :class:`~verta.data_types.NumericValue` type.

    Parameters
    ----------
    comparison : :class:`~verta.common.comparison._VertaComparison`
        Alert condition.

    """

    _TYPE = _AlertService.AlerterTypeEnum.FIXED

    def __init__(self, comparison):
        if not isinstance(comparison, comparison_module._VertaComparison):
            raise TypeError(
                "`comparison` must be an object from verta.common.comparison,"
                " not {}".format(type(comparison))
            )

        self._comparison = comparison

    def __repr__(self):
        return "<fixed alerter ({})>".format(self._comparison)

    def _as_proto(self):
        return _AlertService.AlertFixed(
            threshold=self._comparison.value,
            operator=self._comparison._operator_as_proto(),
        )


class ReferenceAlerter(_Alerter):
    _TYPE = _AlertService.AlerterTypeEnum.REFERENCE

    def __init__(self, threshold, reference_sample):
        self._threshold = threshold
        self._reference_sample_id = utils.extract_id(reference_sample)

    def _as_proto(self):
        return _AlertService.AlertReference(
            threshold=self._threshold,
            reference_sample_id=self._reference_sample_id,
        )
