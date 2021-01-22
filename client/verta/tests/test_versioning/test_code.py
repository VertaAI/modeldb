import pytest
import hypothesis
import hypothesis.strategies as st

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
        ("repo_url", "branch", "tag", "commit_hash"),
        [
            (None, None, None, None),
            ("git@github.com:VertaAI/modeldb.git", None, None, None),
            ("git@github.com:VertaAI/modeldb.git", _git_utils.get_git_branch_name("HEAD"), None, None),
            ("git@github.com:VertaAI/modeldb.git", None, _git_utils.get_git_commit_tag("HEAD") or None, None),
            ("git@github.com:VertaAI/modeldb.git", None, None, _git_utils.get_git_commit_hash("HEAD")),
        ],
    )
    def test_autocapture(self, repo_url, branch, tag, commit_hash):
        code_ver = verta.code.Git(
            repo_url=repo_url,
            branch=branch, tag=tag, commit_hash=commit_hash,
        )

        refs = [arg for arg in (branch, tag, commit_hash) if arg]
        ref = refs[0] if refs else _git_utils.get_git_commit_hash("HEAD")
        assert code_ver._msg.git.repo == (repo_url or _git_utils.get_git_remote_url())
        assert code_ver._msg.git.branch == (branch or _git_utils.get_git_branch_name(ref))
        assert code_ver._msg.git.tag == (tag or _git_utils.get_git_commit_tag(ref))
        assert code_ver._msg.git.hash == (commit_hash or _git_utils.get_git_commit_hash(ref))
        assert code_ver._msg.git.is_dirty == _git_utils.get_git_commit_dirtiness(ref)

    @hypothesis.given(
        repo_url=st.one_of(st.none(), st.emails()),
        branch=st.one_of(st.none(), st.text()),
        tag=st.one_of(st.none(), st.text()),
        commit_hash=st.one_of(st.none(), st.text()),
    )
    def test_user_no_autocapture(self, repo_url, branch, tag, commit_hash):
        """Like test_no_autocapture, but with the public `autocapture` param."""
        code_ver = verta.code.Git(
            repo_url=repo_url,
            branch=branch, tag=tag, commit_hash=commit_hash,
            autocapture=False,
        )

        assert code_ver._msg.git.repo == (repo_url or "")
        assert code_ver._msg.git.branch == (branch or "")
        assert code_ver._msg.git.tag == (tag or "")
        assert code_ver._msg.git.hash == (commit_hash or "")


class TestNotebook:
    def test_no_autocapture(self):
        code_ver = verta.code.Notebook(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )
