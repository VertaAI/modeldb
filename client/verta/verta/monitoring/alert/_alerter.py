# -*- coding: utf-8 -*-

import abc

from verta.external import six

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta._internal_utils import arg_handler
from verta.monitoring import comparison as comparison_module


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _Alerter(object):
    """Base class for an alerter. Not for external use."""

    _TYPE = _AlertService.AlerterTypeEnum.UNKNOWN

    def __repr__(self):
        return "<{} alert>".format(
            _AlertService.AlerterTypeEnum.AlerterType.Name(self._TYPE).lower()
        )

    @classmethod
    @abc.abstractmethod
    def _get_proto_class(cls):
        raise NotImplementedError

    @classmethod
    @abc.abstractmethod
    def _get_alert_field(cls):
        raise NotImplementedError

    @abc.abstractmethod
    def _as_proto(self):
        raise NotImplementedError

    @classmethod
    @abc.abstractmethod
    def _from_proto_msg(cls):
        raise NotImplementedError

    @classmethod
    def _from_proto(cls, msg):
        _protos_to_classes = {
            subcls._get_proto_class(): subcls for subcls in cls.__subclasses__()
        }
        if type(msg) in _protos_to_classes:
            return _protos_to_classes[type(msg)]._from_proto_msg(msg)
        else:
            raise ValueError("unrecognized alerter type {}".format(type(msg)))

    @staticmethod
    def _validate_comparison(comparison):
        if not isinstance(comparison, comparison_module._VertaComparison):
            raise TypeError(
                "`comparison` must be an object from verta.monitoring.comparison,"
                " not {}".format(type(comparison))
            )
        return comparison


class FixedAlerter(_Alerter):
    """
    Compare summary samples with a fixed numerical threshold.

    .. note::

        This alerter is only intended to work with summary samples of the
        :class:`~verta.data_types.NumericValue` type.

    Parameters
    ----------
    comparison : :mod:`~verta.monitoring.comparison`
        Alert condition. An alert is active if a queried sample meets this
        condition.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.comparison import GreaterThan
        from verta.monitoring.alert import FixedAlerter

        alerter = FixedAlerter(GreaterThan(.7))

        alert = summary.alerts.create(
            name="MSE",
            alerter=alerter,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.AlerterTypeEnum.FIXED

    def __init__(self, comparison):
        self._comparison = _Alerter._validate_comparison(comparison)

    def __repr__(self):
        return "<fixed alerter ({})>".format(self._comparison)

    @classmethod
    def _get_proto_class(self):
        return _AlertService.AlertFixed

    @classmethod
    def _get_alert_field(cls):
        return "alerter_fixed"

    @property
    def comparison(self):
        return self._comparison

    def _as_proto(self):
        return _AlertService.AlertFixed(
            threshold=self._comparison.value,
            operator=self._comparison._operator_as_proto(),
        )

    @staticmethod
    def _from_proto_msg(msg):
        comparison = comparison_module._VertaComparison._from_proto(
            msg.operator,
            msg.threshold,
        )
        return FixedAlerter(comparison)


class ReferenceAlerter(_Alerter):
    """Compare differences between samples and a reference against a threshold.

    Parameters
    ----------
    comparison : :mod:`~verta.monitoring.comparison`
        Alert condition. An alert is active if the difference between a
        queried sample and `reference_sample` meets this condition.
    reference_sample : :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
        An existing summary sample to compare queried samples with.

    Examples
    --------
    .. code-block:: python

        from verta import Client
        from verta.monitoring.comparison import GreaterThan
        from verta.monitoring.alert import ReferenceAlerter
        from verta.monitoring.summaries.queries import SummarySampleQuery

        ref_sample = summary.find_samples(SummarySampleQuery(ids=[123]))[0]
        alerter = ReferenceAlerter(
            GreaterThan(.7),
            ref_sample,
        )

        alert = summary.alerts.create(
            name="MSE",
            alerter=alerter,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.AlerterTypeEnum.REFERENCE

    def __init__(self, comparison, reference_sample):
        self._comparison = _Alerter._validate_comparison(comparison)
        self._reference_sample_id = arg_handler.extract_id(reference_sample)

    @property
    def comparison(self):
        return self._comparison

    @classmethod
    def _get_proto_class(self):
        return _AlertService.AlertReference

    @classmethod
    def _get_alert_field(cls):
        return "alerter_reference"

    def _as_proto(self):
        return _AlertService.AlertReference(
            threshold=self._comparison.value,
            reference_sample_id=self._reference_sample_id,
            operator=self._comparison._operator_as_proto(),
        )

    @classmethod
    def _from_proto_msg(cls, msg):
        comparison = comparison_module._VertaComparison._from_proto(
            msg.operator,
            msg.threshold,
        )
        return ReferenceAlerter(comparison, msg.reference_sample_id)


class RangeAlerter(_Alerter):
    """
    Compare summary samples with a fixed numerical range.

    .. note::

        This alerter is only intended to work with summary samples of the
        :class:`~verta.data_types.NumericValue` type.

    Parameters
    ----------
    lower_bound : float
        The lower bound of this numeric range
    upper_bound : float
        The upper bound of this numeric range
    alert_if_outside_range : bool
        Whether to alert on values falling outside of or inside of this range
    Examples
    --------
    .. code-block:: python

        from verta.monitoring.alert import RangeAlerter

        alert = monitored_entity.alerts.create(
            name="positive_coin_flips",
            alerter=RangeAlerter(0.45, 0.55),
            summary_sample_query=sample_query,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.AlerterTypeEnum.RANGE

    def __init__(self, lower_bound, upper_bound, alert_if_outside_range=True):
        self._lower_bound = lower_bound
        self._upper_bound = upper_bound
        self._alert_if_outside_range = alert_if_outside_range

    @property
    def lower_bound(self):
        return self._lower_bound

    @property
    def upper_bound(self):
        return self._upper_bound

    @property
    def alert_if_outside_range(self):
        return self._alert_if_outside_range

    def __repr__(self):
        return "<range alerter ({}, {})>".format(self.lower_bound, self.upper_bound)

    @classmethod
    def _get_proto_class(self):
        return _AlertService.AlertRange

    @classmethod
    def _get_alert_field(cls):
        return "alerter_range"

    def _as_proto(self):
        return _AlertService.AlertRange(
            lower_bound=self.lower_bound,
            upper_bound=self.upper_bound,
            alert_if_outside_range=self.alert_if_outside_range,
        )

    @classmethod
    def _from_proto_msg(cls, msg):
        return cls(msg.lower_bound, msg.upper_bound, msg.alert_if_outside_range)
