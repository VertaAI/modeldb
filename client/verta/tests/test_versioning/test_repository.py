import pytest
import requests

from .. import utils

import verta.dataset
import verta.environment
from verta._internal_utils import _utils


class TestRepository:
    def test_get_by_name(self, client, repository):
        retrieved_repo = client.get_or_create_repository(repository.name)
        assert repository.id == retrieved_repo.id

    def test_get_by_id(self, client, repository):
        retrieved_repo = client.get_or_create_repository(id=repository.id)
        assert repository.id == retrieved_repo.id

    def test_max_one_arg_for_get_commit(self, repository):
        with pytest.raises(ValueError):
            repository.get_commit(branch="banana", tag="coconut")

    def test_master_branch_default(self, repository):
        assert repository.get_commit().branch_name == "master"


class TestCommit:
    def test_add_get_rm(self, commit):
        blob1 = verta.environment.Python(["a==1"])
        blob2 = verta.environment.Python(["b==2"])
        path1 = "path/to/bananas"
        path2 = "path/to/still-bananas"

        commit.update(path1, blob1)
        commit.update(path2, blob2)
        assert commit.get(path1) == blob1
        assert commit.get(path2) == blob2

        commit.remove(path1)
        with pytest.raises(LookupError):
            commit.remove(path1)

        commit.remove(path2)
        assert not commit._blobs

    def test_save(self, commit):
        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit.update(path, blob)

        commit.save(message="banana")

        assert commit.id

    def test_walk(self, commit):
        pytest.importorskip("boto3")

        commit.update("file-1", verta.dataset.S3("s3://verta-starter/census-test.csv"))
        commit.update("a/file-2", verta.dataset.S3("s3://verta-starter/census-train.csv"))
        commit.update("a/file-3", verta.dataset.S3("s3://verta-starter/imdb_master.csv"))
        commit.update("a/b/file-4", verta.dataset.S3("s3://verta-starter/reviews.ann"))
        commit.update("a/c/file-5", verta.dataset.S3("s3://verta-starter/spam.csv"))
        commit.save(message="banana")

        # alphabetical order
        walk = commit.walk()
        assert ("", ["a"], ["file-1"]) == next(walk)
        assert ("a", ["b", "c"], ["file-2", "file-3"]) == next(walk)
        assert ("a/b", [], ["file-4"]) == next(walk)
        assert ("a/c", [], ["file-5"]) == next(walk)

        # mutable `folder_names`
        walk = commit.walk()
        next(walk)  # root
        del next(walk)[1][1]  # remove a/c from a's `folder_names`
        next(walk)  # a/b
        with pytest.raises(StopIteration):
            next(walk)  # a/c removed

    def test_get_commit_by_tag(self, repository):
        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit = repository.get_commit()
        commit.update(path, blob)
        commit.save(message="banana")
        commit.tag("banana")

        assert commit.id == repository.get_commit(tag="banana").id

    def test_get_commit_by_id(self, repository):
        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit = repository.get_commit()
        commit.update(path, blob)
        commit.save(message="banana")

        assert commit.id == repository.get_commit(id=commit.id).id

    def test_become_child(self, commit):
        blob1 = verta.environment.Python(["a==1"])
        blob2 = verta.environment.Python(["b==2"])
        path1 = "path/to/bananas"
        path2 = "path/to/still-bananas"

        commit.update(path1, blob1)
        commit.save(message="banana")
        original_id = commit.id
        commit.update(path2, blob2)

        assert commit.id is None
        assert original_id in commit._parent_ids
        assert commit.get(path1)

        commit.save(message="banana")

        assert commit.id != original_id

    def test_log(self, repository):
        r"""
        Tests the log for this commit tree:

             (master)
              /     \
        (master~) (branch)
              \     /
              (root)

        """
        master = repository.get_commit(branch="master")

        commit_ids = [master.id]

        branch = repository.get_commit(branch="master").new_branch("branch")

        master.update("a", verta.environment.Python(["a==1"]))
        master.save("a")
        commit_ids.append(master.id)

        branch.update("b", verta.environment.Python(["b==2"]))
        branch.save("b")
        commit_ids.append(branch.id)

        master.merge(branch)
        commit_ids.append(master.id)

        for log_commit, expected_id in zip(master.log(), reversed(commit_ids)):
            assert log_commit.id == expected_id

        # use parent of updated-but-unsaved commit
        master.update("c", verta.environment.Python(["c==3"]))
        assert master.id is None  # unsaved
        for log_commit, expected_id in zip(master.log(), reversed(commit_ids)):
            assert log_commit.id == expected_id

    def test_merge_conflict(self, repository):
        branch_a = repository.get_commit(branch="master").new_branch("a")
        branch_a.update("env", verta.environment.Python(["pytest==1"]))
        branch_a.save("a")

        branch_b = repository.get_commit(branch="master").new_branch("b")
        branch_b.update("env", verta.environment.Python(["pytest==2"]))
        branch_b.save("b")

        with pytest.raises(RuntimeError):
            branch_b.merge(branch_a)

    def test_revert(self, repository):
        blob1 = verta.environment.Python(["pytest==1"])
        blob2 = verta.environment.Python(["pytest==2"])
        loc1 = "loc1"
        loc2 = "loc2"

        commit = repository.get_commit(branch="master")

        commit.update(loc1, blob1)
        commit.save("1")
        commit_to_revert_id = commit.id

        commit.update(loc2, blob2)
        commit.save("2")

        commit_to_revert = repository.get_commit(id=commit_to_revert_id)
        commit.revert(commit_to_revert)

        # blob1 removed
        with pytest.raises(LookupError):
            commit.get(loc1)
        # blob2 still present
        assert commit.get(loc2)

    def test_revert_merge_commit(self, repository):
        blob1 = verta.environment.Python(["pytest==1"])
        blob2 = verta.environment.Python(["pytest==2"])
        loc1 = "loc1"
        loc2 = "loc2"

        commit_a = repository.get_commit("master").new_branch("a")
        commit_a.update(loc1, blob1)
        commit_a.save("some message")

        commit_b = repository.get_commit("master").new_branch("b")
        commit_b.update(loc2, blob2)
        commit_b.save("other message")

        commit_a.merge(commit_b)
        commit_a.revert()

        # blob2 removed
        with pytest.raises(LookupError):
            commit_a.get(loc2)

        # blob1 still present
        assert commit_a.get(loc1)

    def test_log_to_run(self, experiment_run, commit):
        blob1 = verta.dataset.Path(__file__)
        reqs = verta.environment.Python.read_pip_environment()
        blob2 = verta.environment.Python(reqs)
        path1 = "data/1"
        path2 = "env/1"

        commit.update(path1, blob1)
        commit.update(path2, blob2)
        commit.save(message="banana")

        key_paths = {'my machine': path1}
        experiment_run.log_commit(commit, key_paths)

        retrieved_commit, retrieved_key_paths = experiment_run.get_commit()
        assert retrieved_commit.id == commit.id
        assert retrieved_key_paths == key_paths

    @pytest.mark.not_oss
    def test_log_to_run_diff_workspaces(self, client, experiment_run, organization):
        repository_name = _utils.generate_default_name()
        repository = client.get_or_create_repository(repository_name, workspace=organization.name)

        # TODO: Uncomment this check when repository.workspace is implemented
        # assert repository.workspace != experiment_run.workspace

        commit = repository.get_commit()
        experiment_run.log_commit(commit)

        retrieved_commit, retrieved_key_paths = experiment_run.get_commit()
        assert retrieved_commit.id == commit.id

        repository.delete()

    def test_log_to_run_diff_workspaces_no_access_error(self, client_2, experiment_run):
        repository_name = _utils.generate_default_name()
        repository = client_2.get_or_create_repository(repository_name)
        commit = repository.get_commit()

        with pytest.raises(requests.HTTPError) as excinfo:
            experiment_run.log_commit(commit)

        excinfo_value = str(excinfo.value).strip()
        assert "403" in excinfo_value

        repository.delete()


class TestBranch:
    def test_set(self, repository):
        branch = "banana"

        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit = repository.get_commit()
        commit.update(path, blob)
        commit.save(message="banana")
        commit = commit.new_branch(branch)

        assert repository.get_commit(branch=branch).id == commit.id

    def test_change(self, repository):
        branch = "banana"

        blob1 = verta.environment.Python(["a==1"])
        blob2 = verta.environment.Python(["b==2"])
        path1 = "path/to/bananas"
        path2 = "path/to/still-bananas"

        commit1 = repository.get_commit()
        root_id = commit1.id
        commit1.update(path1, blob1)
        commit1.save(message="banana")

        commit2 = repository.get_commit(id=root_id)
        commit2.update(path2, blob2)
        commit2.save(message="banana")

        commit1 = commit1.new_branch(branch)
        commit2 = commit2.new_branch(branch)

        assert repository.get_commit(branch=branch).id == commit2.id

    def test_update(self, repository):
        branch = "banana"

        blob1 = verta.environment.Python(["a==1"])
        blob2 = verta.environment.Python(["b==2"])
        path1 = "path/to/bananas"
        path2 = "path/to/still-bananas"

        commit = repository.get_commit()
        commit.update(path1, blob1)
        commit.save(message="banana")
        original_id = commit.id

        commit = commit.new_branch(branch)

        commit.update(path2, blob2)
        commit.save(message="banana")

        assert commit.id != original_id
        assert repository.get_commit(branch=branch).id == commit.id
