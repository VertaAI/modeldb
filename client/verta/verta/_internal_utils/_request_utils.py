# -*- coding: utf-8 -*-

import os
import shutil
import tempfile
import zipfile

from . import (
    _artifact_utils,
    _file_utils,
)


# TODO: migrate request utils from _utils


def download_response(response, chunk_size=_artifact_utils._32MB):
    """
    Downloads the contents of `response` to a temporary file.

    The caller is responsible for deleting the file if needed.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response. May or may not be streamed.
    chunk_size : int, default 32 MB
        Number of bytes to download at a time.

    Returns
    -------
    filepath : str
        Path to temporary file where `response`'s contents were downloaded to.

    """
    tempf = None
    try:
        with tempfile.NamedTemporaryFile('wb', delete=False) as tempf:
            for chunk in response.iter_content(chunk_size=chunk_size):
                tempf.write(chunk)
    except Exception as e:
        # delete partially-downloaded file
        if tempf is not None and os.path.isfile(tempf.name):
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
        Whether to download to `filepath`-as-passed, even if that file already
        exists. If ``False``, `filepath` will be changed to avoid an overwrite.

    Returns
    -------
    filepath : str
        Path where `response`'s contents were downloaded to.

    """
    temp_filepath = download_response(response)
    try:
        if not overwrite_ok:
            # prevent overwrite in case `filepath` was taken during download
            filepath = _file_utils.without_collision(filepath)

        # move written contents to `filepath`
        shutil.move(temp_filepath, filepath)
    except Exception as e:
        os.remove(temp_filepath)
        raise e
    print("download complete; file written to {}".format(filepath))

    return filepath


def download_zipped_dir(response, dirpath, overwrite_ok=False):
    """
    Downloads and unzips the contents of `response` into `dirpath`.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response. May or may not be streamed.
    dirpath : str
        Path to download and unzip `response`'s contents into.
    overwrite_ok : bool, default False
        Whether to extract into `dirpath`-as-passed, even if that directory
        already exists. If ``False``, `dirpath` will be changed to avoid
        collisions.

    Returns
    -------
    dirpath : str
        Path where `response`'s contents were unzipped into.

    """
    # move written contents to `dirpath`
    temp_filepath = download_response(response)
    try:
        if not overwrite_ok:
            # prevent overwrite in case `dirpath` was taken during download
            dirpath = _file_utils.without_collision(dirpath)

        # extract request contents to `dirpath`
        with zipfile.ZipFile(temp_filepath, 'r') as zipf:
            zipf.extractall(dirpath)
    finally:
        os.remove(temp_filepath)
    print("download complete; directory extracted to {}".format(dirpath))

    return dirpath
