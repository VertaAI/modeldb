# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class GreaterThanOrEqualTo(_VertaComparison):
    """
    A comparison: greater than or equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.common.comparison import GreaterThanOrEqualTo
        GreaterThanOrEqualTo(.7)

    """

    _OPERATOR = _CommonService.OperatorEnum.GTE
    _SYMBOL = ">="
