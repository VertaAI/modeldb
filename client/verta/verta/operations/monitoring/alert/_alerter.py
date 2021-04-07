# -*- coding: utf-8 -*-

import abc

from ....external import six

from ...._protos.public.monitoring import Alert_pb2 as _AlertService


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
    _TYPE = _AlertService.AlerterTypeEnum.FIXED

    def __init__(self, threshold):
        self._threshold = threshold

    def _as_proto(self):
        return _AlertService.AlertFixed(
            threshold=self._threshold,
        )


class ReferenceAlerter(_Alerter):
    _TYPE = _AlertService.AlerterTypeEnum.REFERENCE

    def __init__(self, threshold, reference_sample):
        self._threshold = threshold
        self._reference_sample = reference_sample

    def _as_proto(self):
        return _AlertService.AlertReference(
            threshold=self._threshold,
            reference_sample_id=self._reference_sample.id,
        )
