# -*- coding: utf-8 -*-

from verta._protos.public.registry import StageService_pb2

from . import _stage_change


class Development(_stage_change._StageChange):
    """The model version is in active development.

    Parameters
    ----------
    comment : str, optional
        Comment associated with this stage change.

    Attributes
    ----------
    comment : str or None
        Comment associated with this stage change.

    Examples
    --------
    .. code-block:: python

        from verta.registry.stage_change import Development

        model_ver.change_stage(Development("Working on it."))
        model_ver.stage
        # "development"

    """

    _STAGE = StageService_pb2.StageEnum.DEVELOPMENT
