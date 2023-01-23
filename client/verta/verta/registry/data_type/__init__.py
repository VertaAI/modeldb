# -*- coding: utf-8 -*-
"""Data type for registered models."""

from verta._internal_utils import documentation

from ._data_type import _DataType
from ._other import Other
from ._audio import Audio
from ._image import Image
from ._tabular import Tabular
from ._text import Text
from ._video import Video
from ._unknown import _Unknown

documentation.reassign_module(
    [
        Other,
        Audio,
        Image,
        Tabular,
        Text,
        Video,
        _Unknown,
    ],
    module_name=__name__,
)

other = Other()
audio = Audio()
image = Image()
tabular = Tabular()
text = Text()
video = Video()
unknown = _Unknown()
