# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class LessThanOrEqualTo(_VertaComparison):
    """
    A comparison: less than or equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.common.comparison import LessThanOrEqualTo
        LessThanOrEqualTo(.3)

    """

    _OPERATOR = _CommonService.OperatorEnum.LTE
    _SYMBOL = "<="
