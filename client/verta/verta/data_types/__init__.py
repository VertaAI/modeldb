# -*- coding: utf-8 -*-
"""Rich and structured data types."""

from verta._internal_utils import documentation

# import base class first to avoid circular import
from ._verta_data_type import _VertaDataType
from ._confusion_matrix import ConfusionMatrix
from ._discrete_histogram import DiscreteHistogram
from ._float_histogram import FloatHistogram
from ._line import Line
from ._matrix import Matrix
from ._numeric_value import NumericValue
from ._string_value import StringValue
from ._table import Table


documentation.reassign_module(
    [
        ConfusionMatrix,
        DiscreteHistogram,
        FloatHistogram,
        Line,
        Matrix,
        NumericValue,
        StringValue,
        Table,
    ],
    module_name=__name__,
)
