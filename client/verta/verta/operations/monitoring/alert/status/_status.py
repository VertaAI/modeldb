# -*- coding: utf-8 -*-

import abc

from .....external import six

from ....._protos.public.monitoring import Alert_pb2 as _AlertService


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _AlertStatus(object):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.UNKNOWN


class Alerting(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.ALERTING


class Ok(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.OK
