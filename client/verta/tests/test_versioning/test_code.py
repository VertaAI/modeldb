import pytest
import hypothesis
import hypothesis.strategies as st

import itertools

from google.protobuf import json_format

import verta.code
from verta._internal_utils import _git_utils


# check if in git repo
try:
    _git_utils.get_git_repo_root_dir()
except OSError:
    IN_GIT_REPO = False
else:
    IN_GIT_REPO = True

def get_git_test_autocapture_cases():
    """
    Arguments to `Git()` with autocapture on (default) must satisfy these conditions:

        1) `repo_url` can be None or str
        2) `branch`, `tag`, and `commit_hash` can each be None or str
            a) but only one of the three can be non-None
        3) `is_dirty` can be None, True, or False

    """
    if not IN_GIT_REPO:
        return []

    valid_values = [
        [None, _git_utils.get_git_remote_url(), "foo"],  # repo_url
        [None, _git_utils.get_git_branch_name("HEAD")],  # branch
        [None, _git_utils.get_git_commit_tag("HEAD") or None],  # tag (None if HEAD is not at a tag)
        [None, _git_utils.get_git_commit_hash("HEAD")],  # commit_hash
        [None, True, False],  # is_dirty
    ]
    cases = list(itertools.product(*valid_values))
    # remove dups, but maintain consistent original order for pytest-xdist
    cases = sorted(set(cases), key=cases.index)
    # only keep cases if they satisfy (2a)
    cases = [
        case for case in cases
        if sum(val is not None for val in case[1:4]) <= 1
    ]

    return cases


class TestGit:
    def test_no_autocapture(self):
        code_ver = verta.code.Git(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        ).get('git')  # may be {'git': {}} if fields are manually set to empty

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        try:
            _git_utils.get_git_repo_root_dir()
        except OSError:
            pytest.skip("not in git repo")

        code_ver = verta.code.Git()

        assert code_ver.__repr__()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    @pytest.mark.parametrize(
        ("repo_url", "branch", "tag", "commit_hash", "is_dirty"),
        get_git_test_autocapture_cases(),
    )
    def test_autocapture(self, repo_url, branch, tag, commit_hash, is_dirty):
        code_ver = verta.code.Git(
            repo_url=repo_url,
            branch=branch, tag=tag, commit_hash=commit_hash, is_dirty=is_dirty,
        )

        refs = [arg for arg in (branch, tag, commit_hash) if arg]
        ref = refs[0] if refs else _git_utils.get_git_commit_hash("HEAD")
        assert code_ver.repo_url == (repo_url or _git_utils.get_git_remote_url())
        assert code_ver.branch == (branch or _git_utils.get_git_branch_name(ref))
        assert code_ver.tag == (tag or _git_utils.get_git_commit_tag(ref) or None)  # None if HEAD is not at a tag
        assert code_ver.commit_hash == (commit_hash or _git_utils.get_git_commit_hash(ref))
        assert code_ver.is_dirty == (is_dirty if is_dirty is not None else _git_utils.get_git_commit_dirtiness(ref))

    @hypothesis.given(
        repo_url=st.one_of(st.none(), st.emails()),
        branch=st.one_of(st.none(), st.text()),
        tag=st.one_of(st.none(), st.text()),
        commit_hash=st.one_of(st.none(), st.text()),
        is_dirty=st.one_of(st.none(), st.booleans()),
    )
    def test_user_no_autocapture(self, repo_url, branch, tag, commit_hash, is_dirty):
        """Like test_no_autocapture, but with the public `autocapture` param."""
        code_ver = verta.code.Git(
            repo_url=repo_url,
            branch=branch, tag=tag, commit_hash=commit_hash, is_dirty=is_dirty,
            autocapture=False,
        )

        assert code_ver.repo_url == (repo_url or None)
        assert code_ver.branch == (branch or None)
        assert code_ver.tag == (tag or None)
        assert code_ver.commit_hash == (commit_hash or None)
        assert code_ver.is_dirty == (is_dirty or False)


class TestNotebook:
    def test_no_autocapture(self):
        code_ver = verta.code.Notebook(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )
