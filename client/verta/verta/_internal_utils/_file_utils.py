# -*- coding: utf-8 -*-

import os


# TODO: migrate file utils from _utils and _artifact_utils


def increment_filepath(filepath):
    """
    Adds or increments a number near the end of `filepath` to support avoiding collisions.

    .. note::

        If `filepath` has multiple extensions , the number will be placed before the final one (see
        **Examples**). This is consistent with how the Python ``wget`` library avoids collisions
        and how macOS names file copies.

    Parameters
    ----------
    filepath : str
        Filepath.

    Returns
    -------
    new_filepath : str
        Filepath with an added or incremented number.

    Examples
    --------
    .. code-block:: python

        increment_filepath("data.csv")
        # data 1.csv
        increment_filepath("data 1.csv")
        # data 2.csv
        increment_filepath("archive.tar.gz")
        # archive.tar 1.gz

    """
    filebase, ext = os.path.splitext(filepath)

    # check if filename already has number
    if ' ' in filebase:
        original_filebase, number_str = filebase.rsplit(' ', maxsplit=1)
        if number_str.isdigit():
            # increment number
            number = int(number_str) + 1
            return original_filebase + " {}".format(number) + ext

    # add number
    return filebase + " 1" + ext
