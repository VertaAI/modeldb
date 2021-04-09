# -*- coding: utf-8 -*-
import collections

from verta.data_types import (
    DiscreteHistogram,
    FloatHistogram,
)

import numpy as np


class Profiler(object):
    def __init__(self, columns):
        self.columns = columns

    def profile(self, df):
        return {column: histogram for (column, histogram) in self._profile_columns(df)}

    # compatibility with model machinery
    def predict(self, df):
        return self.profile(df)

    def _profile_columns(self, df):
        return [self._profile_column(df, column) for column in self.columns]

    def _profile_column(self, df, column):
        raise NotImplementedError("")

    def __name__(self):
        return "Profiler"

class MissingValuesProfiler(Profiler):
    def __init__(self, columns):
        super(MissingValuesProfiler, self).__init__(columns)
        # if len(columns) > 1:
        #     raise Exception("MissingValues can only be computed on a single column")

    def _profile_column(self, df, column):
        total = df.shape[0]
        try:
            missing = sum(df[column].isnull())
        except KeyError:  # pandas raises this if the column doesn't exist
            missing = total

        return (column + "_missing", DiscreteHistogram(["present", "missing"], [total - missing, missing]))


class BinaryHistogramProfiler(Profiler):
    def __init__(self, columns):
        super(BinaryHistogramProfiler, self).__init__(columns)

    def _profile_column(self, df, column):
        content = df[column].value_counts()
        keys = list(content.keys())
        values = [content[k] for k in keys]
        values = [v.item() for v in values]
        return (column + "_histogram", DiscreteHistogram(values, keys))


class ContinuousHistogramProfiler(Profiler):
    def __init__(self, columns, bins=10):
        super(ContinuousHistogramProfiler, self).__init__(columns)
        self._bins = bins


    def _profile_column(self, df, column):
        if isinstance(self._bins, collections.Mapping):
            bins = self._bins[column]
        else:
            bins = self._bins
        values, limits = np.histogram(df[column], bins=bins)
        values = [v.item() for v in values]
        limits = [lim.item() for lim in limits]
        return (column + "_histogram", FloatHistogram(limits, values))
