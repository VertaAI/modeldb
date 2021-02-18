# -*- coding: utf-8 -*-

import os
import shutil
import tempfile

from . import _file_utils


# TODO: migrate request utils from _utils


def download_response(response, chunk_size=32*(10**6)):
    """
    Downloads the contents of `response` to a temporary file.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response. May or may not be streamed.
    chunk_size : int, default 32 MB
        Number of bytes to download at a time.

    Returns
    -------
    filepath : str
        Path to temporary file where `responsne`'s contents were downloaded to.

    """
    with tempfile.NamedTemporaryFile('wb', delete=False) as tempf:
        try:
            for chunk in response.iter_content(chunk_size=chunk_size):
                tempf.write(chunk)
        except Exception as e:
            os.remove(tempf.name)
            raise e

    return tempf.name


def download_file(response, filepath, overwrite_ok=False):
    """
    Downloads the contents of `response` to `filepath`.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response. May or may not be streamed.
    filepath : str
        Path to download `response`'s contents to.
    overwrite_ok : bool, default False
        Whether to download to `filepath`-as-passed, even if that file already exists. If
        ``False``, `filepath` will be changed to avoid an overwrite.

    Returns
    -------
    filepath : str
        Path where `response`'s contents were downloaded to.

    """
    if not overwrite_ok:
        # prevent overwrite in case `filepath` was taken during download
        filepath = _file_utils.without_collision(filepath)

    # move written contents to `filepath`
    temp_filepath = download_response(response)
    try:
        shutil.move(temp_filepath, filepath)
    except Exception as e:
        os.remove(temp_filepath.name)
        raise e
    print("download complete; file written to {}".format(filepath))

    return filepath
