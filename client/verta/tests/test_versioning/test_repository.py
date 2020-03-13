import pytest

from .. import utils

import verta.dataset
import verta.environment


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
        try:
            commit.tag("banana")

            assert commit.id == repository.get_commit(tag="banana").id
        finally:
            utils.delete_commit(repository.id, commit.id, repository._conn)

    def test_get_commit_by_id(self, repository):
        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit = repository.get_commit()
        commit.update(path, blob)
        commit.save(message="banana")
        try:
            assert commit.id == repository.get_commit(id=commit.id).id
        finally:
            utils.delete_commit(repository.id, commit.id, repository._conn)

    def test_become_child(self, commit):
        blob1 = verta.environment.Python(["a==1"])
        blob2 = verta.environment.Python(["b==2"])
        path1 = "path/to/bananas"
        path2 = "path/to/still-bananas"

        commit.update(path1, blob1)
        commit.save(message="banana")
        original_id = commit.id
        try:
            commit.update(path2, blob2)
            assert commit.id is None
            assert original_id in commit._parent_ids
            assert commit.get(path1)

            commit.save(message="banana")
            assert commit.id != original_id
        finally:
            utils.delete_commit(commit._repo.id, original_id, commit._conn)

    def test_set_parent(self, repository):
        blob1 = verta.environment.Python(["a==1"])
        path1 = "path/to/bananas"

        commit1 = repository.get_commit()
        commit1.update(path1, blob1)
        commit1.save(message="banana")
        try:
            commit2 = repository.new_commit(parents=[commit1])
            assert commit1.id in commit2._parent_ids
            assert commit2.get(path1)
        finally:
            utils.delete_commit(repository.id, commit1.id, repository._conn)

    def test_log_to_run(self, experiment_run, commit):
        blob1 = verta.dataset.Path(__file__)
        blob2 = verta.environment.Python()
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

class TestBranch:
    def test_set(self, repository):
        branch = "banana"

        blob = verta.environment.Python(["a==1"])
        path = "path/to/bananas"

        commit = repository.get_commit()
        commit.update(path, blob)
        commit.save(message="banana")
        try:
            commit.branch(branch)
            assert repository.get_commit(branch=branch).id == commit.id
        finally:
            utils.delete_commit(repository.id, commit.id, repository._conn)

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
        try:
            commit2 = repository.get_commit(id=root_id)
            commit2.update(path2, blob2)
            commit2.save(message="banana")
            try:
                commit1.branch(branch)

                commit2.branch(branch)
                assert repository.get_commit(branch=branch).id == commit2.id
            finally:
                utils.delete_commit(repository.id, commit2.id, repository._conn)
        finally:
            utils.delete_commit(repository.id, commit1.id, repository._conn)

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
        try:
            commit.branch(branch)

            commit.update(path2, blob2)
            commit.save(message="banana")
            try:
                assert commit.id != original_id
                assert repository.get_commit(branch=branch).id == commit.id
            finally:
                utils.delete_commit(commit._repo.id, commit.id, commit._conn)
        finally:
            utils.delete_commit(commit._repo.id, original_id, commit._conn)
