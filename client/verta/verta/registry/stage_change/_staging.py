# -*- coding: utf-8 -*-

from verta._protos.public.registry import StageService_pb2

from . import _stage_change


class Staging(_stage_change._StageChange):
    """The model version is staged for production.

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

        from verta.registry.stage_change import Staging

        model_ver.change_stage(Staging("Undergoing final testing."))
        model_ver.stage
        # "staging"

    """

    _STAGE = StageService_pb2.StageEnum.STAGING
