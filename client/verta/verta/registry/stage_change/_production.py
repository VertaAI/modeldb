# -*- coding: utf-8 -*-

from verta._protos.public.registry import StageService_pb2

from . import _stage_change


class Production(_stage_change._StageChange):
    """The model version is in production.

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

        from verta.registry.stage_change import Production

        model_ver.change_stage(Production("Rolling out to prod."))
        model_ver.stage
        # "production"

    """

    _STAGE = StageService_pb2.StageEnum.PRODUCTION
