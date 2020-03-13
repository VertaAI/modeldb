# -*- coding: utf-8 -*-

from __future__ import print_function

import os

from .._internal_utils import _git_utils
from .._internal_utils import _utils
from ..dataset import _path
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
    def __init__(self, notebook_path=None):
        if notebook_path is None:
            notebook_path = _utils.get_notebook_filepath()
            try:
                _utils.save_notebook(notebook_path)
            except OSError:
                print("unable to automatically save current Notebook;"
                      " capturing latest checkpoint on disk")

        super(Notebook, self).__init__()

        self._msg.notebook.path.CopyFrom(_path.Path(notebook_path)._msg.path)
        try:
            self._msg.notebook.git_repo.CopyFrom(_git.Git()._msg.git)
            repo_root = _git_utils.get_git_repo_root_dir()
        except OSError:
            # TODO: impl and catch a more specific exception for git calls
            print("unable to capture git environment; skipping")
        else:
            # amend notebook path to be relative to repo root
            file_msg = self._msg.notebook.path.components[0]
            file_msg.path = os.path.relpath(file_msg.path, repo_root)
