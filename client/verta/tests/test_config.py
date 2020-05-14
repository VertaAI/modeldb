import os

import yaml

import pytest

from verta._internal_utils import _config_utils

from . import utils


@pytest.fixture
def within_tempdir(tempdir):
    """Creates and ``cd``s into a nested empty directory."""
    pass # TODO: 5 parent dirs

    pass # TODO: cwd

    pass # TODO: children dirs (should not be picked up)

    pass # TODO: cousin dirs (should not be picked up)

    with utils.chdir():  # TODO: cwd
        yield


@pytest.fixture
def home_config_filepath():
    config_filepath = os.path.join('~', "verta_config.yaml")
    config_filepath = os.path.expanduser(config_filepath)

    with open(config_filepath, 'w') as f:
        yaml.safe_dump({}, f)

    yield config_filepath

    os.remove(config_filepath)


class TestRead:
    def test_merge(self):
        pass

    def test_merge_and_overwrite(self):
        pass

    def test_ignore_children_dirs(self):
        pass


class TestWrite:
    @pytest.mark.parametrize("dirpath", ['~', '.', '..'])
    def test_create_empty(self, within_tempdir, dirpath):
        config_filepath = _config_utils.create_empty_config_file(dirpath)
        try:
            with open(config_filepath, 'r') as f:
                assert yaml.safe_load(f) == {}
        finally:
            os.remove(config_filepath)

    def test_update_closest(self, tempdir):
        config_filepath1 = "~"
        config_filepath2 = "../.."
        config_filepath3 = ".."
