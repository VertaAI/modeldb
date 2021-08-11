# -*- coding: utf-8 -*-
"""Data profilers."""

import abc
import collections

from verta.data_types import (
    DiscreteHistogram,
    FloatHistogram,
)
from verta.external import six
from verta._internal_utils.importer import maybe_dependency


@six.add_metaclass(abc.ABCMeta)
class Profiler(object):
    """Produces summary data types for a data frame.

    A profiler's ``profile`` method accepts a data frame and produces a
    dictionary from profile entry names to summary data types according to its
    ``profile_column`` method.

    Attributes
    ----------
    columns : :obj:`list` of :obj:`str`
        The list of data frame columns which will be profiled.
    """

    def __init__(self, columns):
        self.columns = columns

    def __name__(self):
        """Returns \"Profiler\""""
        return "Profiler"

    def profile(self, df):
        """Profile a pandas data frame and return a dictionary.

        Profiles a pandas data frame and returns a dictionary, the values of
        which will be data types.

        Parameters
        ----------
        df : pandas.core.frame.DataFrame
            A data frame to profile.

        Returns
        -------
        dict
            Mapping from profile column name to profiler data type
        """
        return {column: histogram for (column, histogram) in self._profile_columns(df)}

    def predict(self, df):
        """Delegates to self.profile for internal Verta compatibility."""
        return self.profile(df)

    def _profile_columns(self, df):
        return [self.profile_column(df, column) for column in self.columns]

    @abc.abstractmethod
    def profile_column(self, df, column):
        """Profile a data frame column, returning a name and a summarization.

        Parameters
        ----------
        df : pandas.core.frame.DataFrame
            A data frame to profile.
        column : str
            The column in the data frame to profile.

        Returns
        -------
        dict
            A dictionary from summary data name to data types.
        """
        raise NotImplementedError("")


class MissingValuesProfiler(Profiler):
    """Produces discrete histograms for present and missing values.

    Counts the number of null and non-null values as "present" and "missing"
    values and returns DiscreteHistogram summary data for the specified
    columns.

    Parameters
    ----------
    columns : list of str
        The list of data frame columns which will be profiled.

    Attributes
    ----------
    columns : list of str
        The list of data frame columns which will be profiled.
    """

    def __init__(self, columns):
        super(MissingValuesProfiler, self).__init__(columns)

    def profile_column(self, df, column):
        """Profile a data frame column, returning a name and DiscreteHistogram

        Parameters
        ----------
        df : pandas.core.frame.DataFrame
            A data frame to profile.
        column : str
            The column in the data frame to profile.

        Returns
        -------
        dict
            A dictionary from summary data names to DiscreteHistogram instances.
        """
        total = df.shape[0]
        try:
            missing = sum(df[column].isnull())
        except KeyError:  # pandas raises this if the column doesn't exist
            missing = total

        return (column + "_missing", DiscreteHistogram(["present", "missing"], [total - missing, missing]))

    def profile_point(self, sample, reference):
        if sample is None: # Missing
            return DiscreteHistogram(reference._buckets, [0, 1])
        return DiscreteHistogram(reference._buckets, [1, 0])


# TODO: Rename to CategoricalHistogramProfiler?
class BinaryHistogramProfiler(Profiler):
    """Produces discrete histograms counting values per unique value in columns.

    Produces histogram columns for each unique value in the profiled column and
    counts the number of values for these keys. This should not be used for
    columns containing continuously measured values but should be used when the
    set of possible values in a column is small, e.g. boolean valued columns.

    Parameters
    ----------
    columns : list of str
        The list of data frame columns which will be profiled.

    Attributes
    ----------
    columns : list of str
        The list of data frame columns which will be profiled.
    """

    def __init__(self, columns):
        super(BinaryHistogramProfiler, self).__init__(columns)

    def profile_column(self, df, column):
        """Profile a data frame column, returning a name and DiscreteHistogram

        Parameters
        ----------
        df : pandas.core.frame.DataFrame
            A data frame to profile.
        column : str
            The column in the data frame to profile.

        Returns
        -------
        dict
            A dictionary from summary data names to DiscreteHistogram instances.
        """
        content = df[column].value_counts(dropna=True)
        keys = list(content.keys())
        values = [content[k] for k in keys]
        values = [v.item() for v in values]
        return (column + "_histogram", DiscreteHistogram(keys, values))

    # TODO: consider the case where the data is not in the bucket list
    def profile_point(self, sample, reference):
        buckets = reference._buckets
        data = [0]*len(buckets)

        if sample:
            data[buckets.index(sample)] = 1
        return DiscreteHistogram(buckets, data)


# TODO: Consider design/interface for different bins
class ContinuousHistogramProfiler(Profiler):
    """Produces float histograms counting values according to bin intervals.

    Attributes
    ----------
    columns : list of str
        The list of data frame columns which will be profiled.
    bins
    """

    def __init__(self, columns, bins=10):
        super(ContinuousHistogramProfiler, self).__init__(columns)
        self._bins = bins
        self._np = maybe_dependency("numpy")
        if self._np is None:
            raise ImportError("numpy is not installed; try `pip install numpy`")

    def profile_column(self, df, column):
        if isinstance(self._bins, collections.Mapping):
            bins = self._bins[column]
        else:
            bins = self._bins
        values, limits = self._np.histogram(df[column].dropna(), bins=bins)
        values = values.tolist()
        limits = limits.tolist()
        return (column + "_histogram", FloatHistogram(limits, values))

    # TODO: consider the case where the data is outside of the bucket list
    def profile_point(self, sample, reference):
        values, _ = self._np.histogram([sample], bins=reference._bucket_limits)
        return FloatHistogram(reference._bucket_limits, values)
