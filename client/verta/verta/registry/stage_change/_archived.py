# -*- coding: utf-8 -*-

from verta._protos.public.registry import StageService_pb2

from . import _stage_change


class Archived(_stage_change._StageChange):
    """The model version is archived.

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

        from verta.registry.stage_change import Archived

        model_ver.change_stage(Archived("Deprioritized; keeping for posterity."))
        model_ver.stage
        # "archived"

    """

    _STAGE = StageService_pb2.StageEnum.ARCHIVED
