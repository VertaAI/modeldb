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
    is_dirty : bool, optional
        Whether git status is dirty relative to `commit_hash`. If not provided,
        it will automatically be determined.
    autocapture : bool, default True
        Whether to enable the automatic capturing behavior of parameters above.

    Raises
    ------
    OSError
        If git information cannot automatically be determined.

    Attributes
    ----------
    repo_url : str or None
        Remote repository URL.
    branch : str or None
        Branch name.
    tag : str or None
        Commit tag.
    commit_hash : str or None
        Commit hash.
    is_dirty : bool
        Whether git status was dirty relative to the captured commit.

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
    def __init__(self, repo_url=None, branch=None, tag=None, commit_hash=None, is_dirty=None, autocapture=True, _autocapture=True):
        # TODO: switch all similar blobs from _autocapture to autocapture so they have the same API
        if _autocapture is False:
            autocapture = False

        if autocapture:
            # need a unique commit ref, so only one of these params is allowed
            passed_in_refs = [arg for arg in (branch, tag, commit_hash) if arg is not None]
            if len(passed_in_refs) > 1:
                raise ValueError("cannot specify more than one of `branch`, `tag`, and `commit_hash`")
            elif len(passed_in_refs) == 1:
                ref = passed_in_refs[0]
            else:
                ref = _git_utils.get_git_commit_hash()
        else:
            # user can pass whatever they like
            ref = None

        super(Git, self).__init__()

        if autocapture and ref:
            self._msg.git.repo = repo_url or _git_utils.get_git_remote_url()
            self._msg.git.branch = branch or _git_utils.get_git_branch_name(ref)
            self._msg.git.tag = tag or _git_utils.get_git_commit_tag(ref)
            self._msg.git.hash = _git_utils.get_git_commit_hash(ref)  # use full commit hash
            self._msg.git.is_dirty = is_dirty if is_dirty is not None else _git_utils.get_git_commit_dirtiness(ref)
        else:
            self._msg.git.repo = repo_url or ""
            self._msg.git.branch = branch or ""
            self._msg.git.tag = tag or ""
            self._msg.git.hash = commit_hash or ""
            self._msg.git.is_dirty = is_dirty or False

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

    @property
    def repo_url(self):
        return self._msg.git.repo or None

    @property
    def branch(self):
        return self._msg.git.branch or None

    @property
    def tag(self):
        return self._msg.git.tag or None

    @property
    def commit_hash(self):
        return self._msg.git.hash or None

    @property
    def is_dirty(self):
        return self._msg.git.is_dirty
