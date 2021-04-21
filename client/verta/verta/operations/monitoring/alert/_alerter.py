# -*- coding: utf-8 -*-

import abc

from verta.external import six

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.common import comparison as comparison_module
from .. import utils


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _Alerter(object):
    """Base class for an alerter. Not for external use."""

    _TYPE = _AlertService.AlerterTypeEnum.UNKNOWN

    def __init__(self, comparison):
        if not isinstance(comparison, comparison_module._VertaComparison):
            raise TypeError(
                "`comparison` must be an object from verta.common.comparison,"
                " not {}".format(type(comparison))
            )

        self._comparison = comparison

    def __repr__(self):
        return "<{} alert>".format(
            _AlertService.AlerterTypeEnum.AlerterType.Name(self._TYPE).lower()
        )

    @property
    def comparison(self):
        return self._comparison

    @abc.abstractmethod
    def _as_proto(self):
        raise NotImplementedError

    @staticmethod
    def _from_proto(msg):
        comparison = comparison_module._VertaComparison._from_proto(
            msg.operator,
            msg.threshold,
        )

        if isinstance(msg, _AlertService.AlertFixed):
            return FixedAlerter(comparison)
        elif isinstance(msg, _AlertService.AlertReference):
            return ReferenceAlerter(comparison, msg.reference_sample_id)

        raise ValueError("unrecognized alerter type {}".format(type(msg)))

class FixedAlerter(_Alerter):
    """
    Compare summary samples with a fixed numerical threshold.

    .. note::

        This alerter is only intended to work with summary samples of the
        :class:`~verta.data_types.NumericValue` type.

    Parameters
    ----------
    comparison : :class:`~verta.common.comparison._VertaComparison`
        Alert condition. An alert is active if a queried sample meets this
        condition.

    Examples
    --------
    .. code-block:: python

        from verta.common.comparison import GreaterThan
        from verta.operations.monitoring.alert import FixedAlerter

        alerter = FixedAlerter(GreaterThan(.7))

        alert = monitored_entity.alerts.create(
            name="MSE",
            alerter=alerter,
            summary_sample_query=sample_query,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.AlerterTypeEnum.FIXED

    def __repr__(self):
        return "<fixed alerter ({})>".format(self._comparison)

    def _as_proto(self):
        return _AlertService.AlertFixed(
            threshold=self._comparison.value,
            operator=self._comparison._operator_as_proto(),
        )


class ReferenceAlerter(_Alerter):
    """Compare distances between samples and a reference against a threshold.

    Parameters
    ----------
    comparison : :class:`~verta.common.comparison._VertaComparison`
        Alert condition. An alert is active if the distance between a queried
        sample and `reference_sample` meets this condition.
    reference_sample : :class:`~verta.operations.monitoring.summarySummarySample`
        An existing summary sample to compare queried samples with.

    Examples
    --------
    .. code-block:: python

        from verta import Client
        from verta.common.comparison import GreaterThan
        from verta.operations.monitoring.alert import ReferenceAlerter
        from verta.operations.monitoring.summaries import SummarySampleQuery

        ref_sample = summary.find_samples(SummarySampleQuery(ids=[123]))[0]
        alerter = ReferenceAlerter(
            GreaterThan(.7),
            ref_sample,
        )

        alert = monitored_entity.alerts.create(
            name="MSE",
            alerter=alerter,
            summary_sample_query=sample_query,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.AlerterTypeEnum.REFERENCE

    def __init__(self, comparison, reference_sample):
        super(ReferenceAlerter, self).__init__(comparison)
        self._reference_sample_id = utils.extract_id(reference_sample)

    def _as_proto(self):
        return _AlertService.AlertReference(
            threshold=self._comparison.value,
            reference_sample_id=self._reference_sample_id,
            operator=self._comparison._operator_as_proto(),
        )
