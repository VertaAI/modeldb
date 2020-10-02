# -*- coding: utf-8 -*-

from __future__ import print_function

import os

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from .._internal_utils import _git_utils
from .._internal_utils import _utils
from ..dataset import _dataset, _path
from . import _code
from . import _git


class Notebook(_code._Code):
    """
    Captures metadata about the Jupyter Notebook at `notebook_path` and the current git environment.

    .. note::

        If a git environment is detected, then the Notebook's recorded filepath will be relative to
        the root of the repository.

    Parameters
    ----------
    notebook_path : str, optional
        Filepath of the Jupyter Notebook. If not provided, it will automatically be determined.
    _autocapture : bool, default True
        Whether to enable the automatic capturing behavior of parameters above.

    Raises
    ------
    OSError
        If the Notebook filepath cannot automatically be determined.

    Examples
    --------
    .. code-block:: python

        from verta.code import Notebook
        code1 = Notebook()
        code2 = Notebook("Spam-Detection.ipynb")

    """
    def __init__(self, notebook_path=None, _autocapture=True):
        if notebook_path is None and _autocapture:
            notebook_path = _utils.get_notebook_filepath()
            try:
                _utils.save_notebook(notebook_path)
            except (ImportError, OSError):
                print("unable to automatically save current Notebook;"
                      " capturing latest checkpoint on disk")

        super(Notebook, self).__init__()

        if notebook_path is not None:
            notebook_path = os.path.expanduser(notebook_path)
            notebook_component = _path.Path(notebook_path).list_components()[0]
            self._msg.notebook.path.CopyFrom(notebook_component._as_proto())
            try:
                git_blob = _git.Git()  # do not store as attribute, to avoid data duplication
                repo_root = _git_utils.get_git_repo_root_dir()
            except OSError:
                # TODO: impl and catch a more specific exception for git calls
                print("unable to capture git environment; skipping")
            else:
                self._msg.notebook.git_repo.CopyFrom(git_blob._msg.git)
                # amend notebook path to be relative to repo root
                file_msg = self._msg.notebook.path
                file_msg.path = os.path.relpath(file_msg.path, repo_root)

    def __repr__(self):
        lines = ["Notebook Version"]
        notebook_component_msg = self._msg.notebook.path
        notebook_component = _dataset.Component._from_proto(notebook_component_msg)
        if notebook_component.path:
            lines.extend(repr(notebook_component).splitlines())
        git_msg = self._msg.notebook.git_repo
        if git_msg.hash:
            # re-use Git blob repr
            git_blob = _git.Git(_autocapture=False)
            git_blob._msg.git.CopyFrom(self._msg.notebook.git_repo)
            # this will intentionally add a level of indentation in the final repr
            lines.extend(
                repr(git_blob).splitlines()
            )

        return "\n    ".join(lines)

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls(_autocapture=False)
        obj._msg.CopyFrom(blob_msg.code)

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()
        blob_msg.code.CopyFrom(self._msg)

        return blob_msg
