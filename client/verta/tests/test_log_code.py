import six

import os
import zipfile

import verta
from verta._internal_utils import _git_utils

import pytest
from . import utils


# check if in git repo
try:
    _git_utils.get_git_repo_root_dir()
except OSError:
    IN_GIT_REPO = False
else:
    IN_GIT_REPO = True


class TestLogGit:
    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_git(self, client):
        """git mode succeeds inside git repo"""
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = True

            mdb_entity.log_code()
            code_version = mdb_entity.get_code()

            assert isinstance(code_version, dict)
            assert 'filepaths' in code_version
            assert len(code_version['filepaths']) == 1
            assert __file__.endswith(code_version['filepaths'][0])

            assert code_version['repo_url'] == _git_utils.get_git_remote_url()
            assert code_version['commit_hash'] == _git_utils.get_git_commit_hash("HEAD")
            assert code_version['is_dirty'] == _git_utils.get_git_commit_dirtiness("HEAD")

    def test_log_git_failure(self, client):
        """git mode fails outside git repo"""
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = True

            with utils.chdir('/'):  # assuming the tester's root isn't a git repo
                mdb_entity.log_code()

            with pytest.raises(RuntimeError):
                mdb_entity.get_code()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_git_provide_path(self, client):
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = True

            mdb_entity.log_code(exec_path="conftest.py")
            code_version = mdb_entity.get_code()

            assert isinstance(code_version, dict)
            assert 'filepaths' in code_version
            assert len(code_version['filepaths']) == 1
            assert os.path.abspath("conftest.py").endswith(code_version['filepaths'][0])

            assert code_version['repo_url'] == _git_utils.get_git_remote_url()
            assert code_version['commit_hash'] == _git_utils.get_git_commit_hash("HEAD")
            assert code_version['is_dirty'] == _git_utils.get_git_commit_dirtiness("HEAD")

    @pytest.mark.parametrize(
        ("exec_path", "repo_url", "commit_hash", "is_dirty"),
        [
            (None, None, None, None),
            ("foo", None, None, None),
            (None, "bar", None, None),
            (None, None, "baz", None),
            (None, None, None, True),
            (None, None, None, False),
        ],
    )
    def test_no_autocapture(self, experiment_run, exec_path, repo_url, commit_hash, is_dirty):
        experiment_run._conf.use_git = True

        experiment_run.log_code(
            exec_path=exec_path,
            repo_url=repo_url, commit_hash=commit_hash, is_dirty=is_dirty,
            autocapture=False,
        )
        code_version = experiment_run.get_code()

        assert isinstance(code_version, dict)
        if exec_path:
            assert len(code_version['filepaths']) == 1
            assert code_version['filepaths'][0] == exec_path
        else:
            assert not code_version.get('filepaths')
        assert code_version.get('repo_url') == repo_url
        assert code_version.get('commit_hash') == commit_hash
        assert code_version.get('is_dirty') == is_dirty


class TestLogSource:
    def test_log_script(self, client):
        """source mode succeeds for Python script"""
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = False

            mdb_entity.log_code()
            zipf = mdb_entity.get_code()

            assert isinstance(zipf, zipfile.ZipFile)
            assert len(zipf.namelist()) == 1
            assert __file__.endswith(zipf.namelist()[0])
            assert open(__file__, 'rb').read() == zipf.open(zipf.infolist()[0]).read()

    def test_log_script_provide_path(self, client):
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = False

            mdb_entity.log_code("conftest.py")
            zipf = mdb_entity.get_code()

            assert isinstance(zipf, zipfile.ZipFile)
            assert len(zipf.namelist()) == 1
            assert os.path.abspath("conftest.py").endswith(zipf.namelist()[0])
            assert open("conftest.py", 'rb').read() == zipf.open(zipf.infolist()[0]).read()

    def test_no_autocapture_error(self, experiment_run):
        experiment_run._conf.use_git = False

        with pytest.raises(ValueError, match="autocapture"):
            experiment_run.log_code("conftest.py", autocapture=False)


class TestConflict:
    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_two_git(self, client):
        client._conf.use_git = True

        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity.log_code()

            with pytest.raises(ValueError):
                mdb_entity.log_code()

    def test_log_two_source(self, client):
        client._conf.use_git = False

        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity.log_code()

            with pytest.raises(ValueError):
                mdb_entity.log_code()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_git_then_source(self, client):
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = True
            mdb_entity.log_code()

            mdb_entity._conf.use_git = False
            with pytest.raises(ValueError):
                mdb_entity.log_code()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_source_then_git(self, client):
        client.set_project()
        client.set_experiment()
        for mdb_entity in (client.set_experiment_run(),):
            mdb_entity._conf.use_git = False
            mdb_entity.log_code()

            mdb_entity._conf.use_git = True
            with pytest.raises(ValueError):
                mdb_entity.log_code()


class TestOverwrite:
    @pytest.mark.skip(reason="complex to implement test")
    def test_log_two_git(self, experiment_run):
        experiment_run._conf.use_git = True

        experiment_run.log_code()

        experiment_run.log_code(overwrite=True)

    def test_log_two_source(self, experiment_run):
        experiment_run._conf.use_git = False

        experiment_run.log_code("conftest.py")

        experiment_run.log_code(overwrite=True)
        zipf = experiment_run.get_code()

        assert isinstance(zipf, zipfile.ZipFile)
        assert len(zipf.namelist()) == 1
        assert __file__.endswith(zipf.namelist()[0])
        assert open(__file__, 'rb').read() == zipf.open(zipf.infolist()[0]).read()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_git_then_source(self, experiment_run):
        experiment_run._conf.use_git = True
        experiment_run.log_code()

        experiment_run._conf.use_git = False
        experiment_run.log_code(overwrite=True)
        zipf = experiment_run.get_code()

        assert isinstance(zipf, zipfile.ZipFile)
        assert len(zipf.namelist()) == 1
        assert __file__.endswith(zipf.namelist()[0])
        assert open(__file__, 'rb').read() == zipf.open(zipf.infolist()[0]).read()

    @pytest.mark.skipif(not IN_GIT_REPO, reason="not in git repo")
    def test_log_source_then_git(self, experiment_run):
        experiment_run._conf.use_git = False
        experiment_run.log_code()

        experiment_run._conf.use_git = True
        experiment_run.log_code(overwrite=True)
        code_version = experiment_run.get_code()

        assert isinstance(code_version, dict)
        assert 'filepaths' in code_version
        assert len(code_version['filepaths']) == 1
        assert __file__.endswith(code_version['filepaths'][0])

    def test_proj_error(self, client):
        client._conf.use_git = False

        with pytest.raises(ValueError):
            client.set_project().log_code(overwrite=True)

    def test_expt_error(self, client):
        client._conf.use_git = False
        client.set_project()

        with pytest.raises(ValueError):
            client.set_experiment().log_code(overwrite=True)
