# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Code_pb2 as _CodeService

from .._internal_utils import _git_utils
from . import _code


class Git(_code._Code):
    """
    Captures metadata about the git commit with the specified `branch`, `tag`, or `commit_hash`.

    Parameters
    ----------
    repo_url : str, optional
        Remote repository URL. If not provided, it will automatically be determined.
    branch : str, optional
        Branch name. If not provided, it will automatically be determined.
    tag : str, optional
        Commit tag. If not provided, it will automatically be determined.
    commit_hash : str, optional
        Commit hash. If not provided, it will automatically be determined.

    Raises
    ------
    OSError
        If git information cannot automatically be determined.

    Examples
    --------
    .. code-block:: python

        from verta.code import Git
        code1 = Git()
        code2 = Git(
            repo_url="git@github.com:VertaAI/modeldb.git",
            tag="client-v0.14.0",
        )
        code3 = Git(
            commit_hash="e4e0675",
        )

    """
    def __init__(self, repo_url=None, branch=None, tag=None, commit_hash=None):
        passed_in_refs = [arg for arg in (branch, tag, commit_hash) if arg is not None]
        if len(passed_in_refs) > 1:
            raise ValueError("cannot specify more than one of `branch`, `tag`, and `commit_hash`")
        elif len(passed_in_refs) == 1:
            ref = passed_in_refs[0]
        else:
            ref = _git_utils.get_git_commit_hash()

        super(Git, self).__init__()

        self._msg.git.repo = repo_url or _git_utils.get_git_remote_url()
        self._msg.git.branch = branch or _git_utils.get_git_branch_name(ref)
        self._msg.git.tag = tag or _git_utils.get_git_commit_tag(ref)
        self._msg.git.hash = _git_utils.get_git_commit_hash(ref)  # use full commit hash
        self._msg.git.is_dirty = _git_utils.get_git_commit_dirtiness(ref)
