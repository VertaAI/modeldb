# -*- coding: utf-8 -*-

import os


# TODO: migrate file utils from _utils and _artifact_utils


def increment_path(path):
    """
    Adds or increments a number near the end of `path` to support avoiding collisions.

    .. note::

        If `path` has multiple extensions , the number will be placed before the final one (see
        **Examples**). This is consistent with how the Python ``wget`` library avoids collisions
        and how macOS names file copies.

    Parameters
    ----------
    path : str
        File or directory path.

    Returns
    -------
    new_path : str
        `path` with an added or incremented number.

    Examples
    --------
    .. code-block:: python

        increment_path("data.csv")
        # data 1.csv
        increment_path("data 1.csv")
        # data 2.csv
        increment_path("archive.tar.gz")
        # archive.tar 1.gz

    """
    base, ext = os.path.splitext(path)

    # check if name already has number
    if ' ' in base:
        original_base, number_str = base.rsplit(' ', 1)
        if number_str.isdigit():
            # increment number
            number = int(number_str) + 1
            return original_base + " {}".format(number) + ext

    # add number
    return base + " 1" + ext
