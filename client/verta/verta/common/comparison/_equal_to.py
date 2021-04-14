# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class EqualTo(_VertaComparison):
    """
    A comparison: equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.common.comparison import EqualTo
        EqualTo(.5)

    """

    _OPERATOR = _CommonService.OperatorEnum.EQ
    _SYMBOL = "=="
