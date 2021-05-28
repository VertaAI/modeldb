# pylint: disable=unidiomatic-typecheck

import pathlib2
import pytest
from verta._internal_utils import _file_utils


class TestFileUtils:
    @pytest.mark.parametrize(
        "input_filepath, expected_filepath",
        [
            ("data.csv", "data 1.csv"),
            ("data 1.csv", "data 2.csv"),
            ("my data.csv", "my data 1.csv"),
            ("my data 1.csv", "my data 2.csv"),
            ("archive.tar.gz", "archive.tar 1.gz"),
            ("archive.tar 1.gz", "archive.tar 2.gz"),
            ("my archive.tar.gz", "my archive.tar 1.gz"),
            ("my archive.tar 1.gz", "my archive.tar 2.gz"),
        ],
    )
    def test_increment_path(self, input_filepath, expected_filepath):
        assert expected_filepath == _file_utils.increment_path(input_filepath)

    @pytest.mark.parametrize(
        "path, prefix_dir, expected",
        [
            # simple removal cases
            ("files/data.csv", "files", "data.csv"),
            ("files/data/data.csv", "files/data", "data.csv"),
            # simple no-change cases
            ("files/data.csv", "foo", "files/data.csv"),
            ("files/data.csv", "fil", "files/data.csv"),
            ("files/data/data.csv", "files/data.csv", "files/data/data.csv"),
            # edge cases
            ("data.csv", "data.csv", "data.csv"),
            # examples from comments in fn
            ("data/census-train.csv", "data/census", "data/census-train.csv"),
            ("data/census/train.csv", "data/census", "train.csv"),
            # remove "s3://"
            (
                "s3://verta-starter/census-train.csv",
                "s3:",
                "verta-starter/census-train.csv",
            ),
            (
                "s3://verta-starter/census-train.csv",
                "s3:/",
                "verta-starter/census-train.csv",
            ),
            (
                "s3://verta-starter/census-train.csv",
                "s3://",
                "verta-starter/census-train.csv",
            ),
        ],
    )
    def test_remove_prefix_dir(self, path, prefix_dir, expected):
        assert _file_utils.remove_prefix_dir(path, prefix_dir) == expected

    def test_flatten_file_trees(self, in_tempdir):
        filepaths = {
            "README.md",
            "data/train.csv",
            "data/test.csv",
            "script.py",
            "utils/data/clean.py",
            "utils/misc/misc.py",
        }
        paths = ["README.md", "data", "script.py", "utils"]

        # create files
        for filepath in filepaths:
            filepath = pathlib2.Path(filepath)
            filepath.parent.mkdir(parents=True, exist_ok=True)
            filepath.touch()

        assert _file_utils.flatten_file_trees(paths) == filepaths
