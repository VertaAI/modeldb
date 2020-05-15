import os

import yaml

import pytest

from verta._internal_utils import _config_utils

from . import utils


@pytest.fixture
def config_filetree(tempdir):
    """
    Creates config files and ``cd``s into a nested directory.

    Yields
    ------
    config : dict
        Expected merged config.

    """
    config_filename = "verta_config.yaml"
    config_items = [
        ('email', "hello@verta.ai"),
        ('dev_key', "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"),
        ('host', "app.verta.ai"),
        ('workspace', "My Workspace"),
        ('project', "My Project"),
        ('experiment', "My Experiment"),
        ('dataset', "My Dataset"),
    ]
    config_iter = iter(config_items)

    # home dir
    home_dir = os.path.expanduser('~')
    with open(os.path.join(home_dir, config_filename), 'w') as f:
        key, value = next(config_iter)
        yaml.safe_dump({key: value}, f)
    try:  # delete home config during teardown
        # 5 parent dirs
        curr_dir = tempdir
        for i in reversed(range(5)):
            curr_dir = os.path.join(curr_dir, "parent{}".format(i+1))
            os.mkdir(curr_dir)
            with open(os.path.join(curr_dir, config_filename), 'w') as f:
                key, value = next(config_iter)
                yaml.safe_dump({key: value}, f)

        # cwd-to-be
        curr_dir = os.path.join(curr_dir, "current")
        os.mkdir(curr_dir)
        with open(os.path.join(curr_dir, config_filename), 'w') as f:
            key, value = next(config_iter)
            yaml.safe_dump({key: value}, f)

        # make sure we've used every config item
        with pytest.raises(StopIteration):
            next(config_iter)

        # children dirs (should not be picked up)
        child_dirs = [
            os.path.join(curr_dir, "childA"),
            os.path.join(curr_dir, "childB"),
        ]
        for child_dir in child_dirs:
            os.mkdir(child_dir)
            with open(os.path.join(child_dir, config_filename), 'w') as f:
                yaml.safe_dump({'INVALID_KEY': "INVALID_VALUE"}, f)

        # cousin dirs (should not be picked up)
        cousin_dirs = [
            os.path.join(curr_dir, "..", "..", "..", "cousinA"),
            os.path.join(curr_dir, "..", "..", "cousinB"),
        ]
        for cousin_dir in cousin_dirs:
            os.mkdir(cousin_dir)
            with open(os.path.join(cousin_dir, config_filename), 'w') as f:
                yaml.safe_dump({'INVALID_KEY': "INVALID_VALUE"}, f)

        with utils.chdir(curr_dir):
            yield dict(config_items)
    finally:
        os.remove(os.path.join(home_dir, config_filename))


@pytest.fixture
def home_config_filepath():
    config_filepath = os.path.join('~', "verta_config.yaml")
    config_filepath = os.path.expanduser(config_filepath)

    with open(config_filepath, 'w') as f:
        yaml.safe_dump({}, f)

    yield config_filepath

    os.remove(config_filepath)


class TestRead:
    def test_merge(self, config_filetree):
        with _config_utils.read_config() as config:
            assert config == config_filetree

    def test_merge_and_overwrite(self):
        pass

    def test_ignore_children_dirs(self):
        pass


class TestWrite:
    @pytest.mark.parametrize("dirpath", ['~', '.'])
    def test_create_empty(self, dirpath):
        config_filepath = _config_utils.create_empty_config_file(dirpath)
        try:
            with open(config_filepath, 'r') as f:
                assert yaml.safe_load(f) == {}
        finally:
            os.remove(config_filepath)

    def test_update_closest(self):
        pass
