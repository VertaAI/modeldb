# -*- coding: utf-8 -*-

import os
import shutil
import tempfile

from . import _file_utils


# TODO: migrate request utils from _utils


def download(response, filepath, chunk_size=32*(10**6), overwrite_ok=False):
    """
    Downloads the contents of `response` to `filepath`.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response. May or may not be streamed.
    filepath : str
        Path to download `response`'s contents to.
    chunk_size : int, default 32 MB
        Number of bytes to download at a time.
    overwrite_ok : bool, default False
        Whether to download to `filepath`-as-passed, even if that file already exists. If
        ``False``, `filepath` will be changed to avoid an overwrite.

    Returns
    -------
    filepath : str
        Path where `response`'s contents were downloaded to.

    """
    tempf = None
    try:
        # download response contents into temporary file
        with tempfile.NamedTemporaryFile('wb', delete=False) as tempf:
            for chunk in response.iter_content(chunk_size=chunk_size):
                tempf.write(chunk)

        if not overwrite_ok:
            # prevent overwrite in case `filepath` was taken during download
            filepath = _file_utils.without_collision(filepath)

        # move written contents to `filepath`
        shutil.move(tempf.name, filepath)
    except Exception as e:
        # delete partially-downloaded file
        if tempf is not None and os.path.isfile(tempf.name):
            os.remove(tempf.name)
        raise e
    print("download complete; file written to {}".format(filepath))

    return filepath
