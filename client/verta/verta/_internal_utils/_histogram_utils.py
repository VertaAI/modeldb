# -*- coding: utf-8 -*-

def calculate_binary_histogram(data):
    raise NotImplementedError

def calculate_discrete_histogram(data):
    raise NotImplementedError

def calculate_float_histogram(data, num_bins=10):
    """
    Calculates a histogram for continuous `data`.

    Parameters
    ----------
    data : list-like of float
        Continuous data to be binned.
    num_bins : int, default 10
        Number of bins to use.

    Returns
    -------
    histogram : dict

    """
    # calculate bin boundaries
    start, stop = min(data), max(data)
    space = (stop - start)/num_bins
    bin_boundaries = [start + space*i for i in range(num_bins+1)]

    # fit `data` into bins
    reference_counts = []
    for l, r in zip(bin_boundaries[:-1], bin_boundaries[1:]):
        count = len([datum for datum in data if l <= datum < r])
        reference_counts.append(count)

    return {
        'histogram': {
            'float': {
                'bucket_limits': bin_boundaries,
                'count': reference_counts,
            },
        },
        'type': "float",
    }


class HistogramError(TypeError):
    pass
