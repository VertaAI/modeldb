# -*- coding: utf-8 -*-
"""Action type for registered models."""

from verta._internal_utils import documentation

from ._action_type import _ActionType
from ._other import Other
from ._classification import Classification
from ._clustering import Clustering
from ._detection import Detection
from ._regression import Regression
from ._transcription import Transcription
from ._translation import Translation
from ._unknown import _Unknown

documentation.reassign_module(
    [
        Other,
        Classification,
        Clustering,
        Detection,
        Regression,
        Transcription,
        Translation,
        _Unknown,
    ],
    module_name=__name__,
)

other = Other()
classification = Classification()
clustering = Clustering()
detection = Detection()
regression = Regression()
transcription = Transcription()
translation = Translation()
unknown = _Unknown()
