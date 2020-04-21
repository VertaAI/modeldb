# -*- coding: utf-8 -*-

def calculate_bin_boundaries(data, num_bins=10):
    """
    Calculates boundaries for `num_bins` equally-spaced histogram bins.

    Parameters
    ----------
    data : sequence of numbers
        Continuous data to be binned.
    num_bins : int, default 10
        Number of bins to use.

    Returns
    -------
    bin_boundaries : list of float with length `num_bins`+1
        Boundaries of histogram bins.

    """
    start, stop = min(data), max(data)
    space = (stop - start)/num_bins
    return [start + space*i for i in range(num_bins+1)]


def calculate_reference_counts(data, bin_boundaries):
    """
    Fits `data` into the histogram bins defined by `bin_boundaries`.

    Parameters
    ----------
    data : sequence of numbers
        Numerical data to be binned.
    bin_boundaries : list of float of length N+1
        Boundaries for a histogram's N bins.

    Returns
    -------
    list of float with length N
        Counts of `data` values in each bin defined by `bin_boundaries`.

    Raises
    ------
    ValueError
        If `data` contains a value outside the range defined by `bin_boundaries`.

    """
    # TODO: there is definitely a faster way to do this
    reference_counts = []
    for l, r in zip(bin_boundaries[:-1], bin_boundaries[1:]):
        count = len([datum for datum in data if l <= datum < r])
        reference_counts.append(count)

    return reference_counts
