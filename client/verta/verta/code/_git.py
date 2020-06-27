# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

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
    _autocapture : bool, default True
        Whether to enable the automatic capturing behavior of parameters above.

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
    def __init__(self, repo_url=None, branch=None, tag=None, commit_hash=None, _autocapture=True):
        passed_in_refs = [arg for arg in (branch, tag, commit_hash) if arg is not None]
        if len(passed_in_refs) > 1:
            raise ValueError("cannot specify more than one of `branch`, `tag`, and `commit_hash`")
        elif len(passed_in_refs) == 1:
            ref = passed_in_refs[0]
        elif _autocapture:
            ref = _git_utils.get_git_commit_hash()
        else:
            ref = None

        super(Git, self).__init__()

        if ref is not None:
            self._msg.git.repo = repo_url or _git_utils.get_git_remote_url()
            self._msg.git.branch = branch or _git_utils.get_git_branch_name(ref)
            self._msg.git.tag = tag or _git_utils.get_git_commit_tag(ref)
            self._msg.git.hash = _git_utils.get_git_commit_hash(ref)  # use full commit hash
            self._msg.git.is_dirty = _git_utils.get_git_commit_dirtiness(ref)

    def __repr__(self):
        lines = ["Git Version"]
        if self._msg.git.hash:
            lines.append("{}commit {}".format(
                "dirty " if self._msg.git.is_dirty else "",
                self._msg.git.hash,
            ))
        if self._msg.git.branch:
            lines.append("on branch {}".format(self._msg.git.branch))
        if self._msg.git.tag:
            lines.append("with tag {}".format(self._msg.git.tag))
        if self._msg.git.repo:
            lines.append("in repo {}".format(self._msg.git.repo))

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
