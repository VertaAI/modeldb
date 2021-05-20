import os
import shutil
import tempfile

import yaml

import pytest

from verta._internal_utils import _config_utils

from . import utils


@pytest.fixture
def expected_config(in_tempdir):
    """
    Creates config files in several nested directories, and ``cd``s into one of them.

    Yields
    ------
    config : dict
        Expected merged config.

    """
    config_filename = _config_utils.CONFIG_YAML_FILENAME
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

    # backup existing home config
    home_config_filepath = os.path.join(_config_utils.HOME_VERTA_DIR, config_filename)
    backup_dir = tempfile.mkdtemp(dir=".")
    if not os.path.isdir(_config_utils.HOME_VERTA_DIR):
        os.mkdir(_config_utils.HOME_VERTA_DIR)
    else:
        if os.path.exists(home_config_filepath):
            shutil.move(home_config_filepath, os.path.join(backup_dir, config_filename))
    try:
        # create home config
        with open(home_config_filepath, 'w') as f:
            key, value = next(config_iter)
            yaml.safe_dump({key: value}, f)

        # create config in 5 parent dirs
        curr_dir = "."
        for i in reversed(range(5)):
            curr_dir = os.path.join(curr_dir, "parent{}".format(i+1))
            os.mkdir(curr_dir)
            with open(os.path.join(curr_dir, config_filename), 'w') as f:
                key, value = next(config_iter)
                yaml.safe_dump({key: value}, f)

        # create config in the dir we're going to cd to
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
            os.path.join(curr_dir, '..', '..', '..', "cousinA"),
            os.path.join(curr_dir, '..', '..', "cousinB"),
        ]
        for cousin_dir in cousin_dirs:
            os.mkdir(cousin_dir)
            with open(os.path.join(cousin_dir, config_filename), 'w') as f:
                yaml.safe_dump({'INVALID_KEY': "INVALID_VALUE"}, f)

        with utils.chdir(curr_dir):
            yield dict(config_items)
    finally:
        # delete created home config
        os.remove(home_config_filepath)

        # restore home config
        if os.path.exists(os.path.join(backup_dir, config_filename)):
            shutil.move(os.path.join(backup_dir, config_filename), home_config_filepath)


class TestRead:
    def test_discovery(self, expected_config):
        config_filename = _config_utils.CONFIG_YAML_FILENAME

        # NOTE: make sure this matches the fixture
        expected_config_filepaths = list(map(os.path.abspath, [
            config_filename,
            os.path.join('..', config_filename),
            os.path.join('..', '..', config_filename),
            os.path.join('..', '..', '..', config_filename),
            os.path.join('..', '..', '..', '..', config_filename),
            os.path.join('..', '..', '..', '..', '..', config_filename),
            os.path.join(_config_utils.HOME_VERTA_DIR, config_filename),
        ]))

        assert _config_utils.find_config_files() == expected_config_filepaths

    def test_merge(self, expected_config):
        with _config_utils.read_merged_config() as config:
            assert config == expected_config

    def test_merge_and_overwrite(self, expected_config):
        cwd_config_filepath = os.path.abspath(_config_utils.CONFIG_YAML_FILENAME)

        # pick a key from config
        config_keys = set(expected_config.keys())
        #     don't pick the key that's already in cwd config
        with open(cwd_config_filepath, 'r') as f:
            nearest_config = yaml.safe_load(f)
            config_keys -= set(nearest_config.keys())
        key = config_keys.pop()

        # arbitrary new value
        value = "NEW_VALUE"

        # add item to cwd config
        nearest_config[key] = value
        with open(cwd_config_filepath, 'w') as f:
            yaml.safe_dump(nearest_config, f)

        # merged config has new value, not old value
        expected_config[key] = value
        with _config_utils.read_merged_config() as config:
            assert config == expected_config


class TestWrite:
    @pytest.mark.parametrize("dirpath", ['~', '.'])
    def test_create_empty(self, dirpath):
        config_filepath = _config_utils.create_empty_config_file(dirpath)
        try:
            with open(config_filepath, 'r') as f:
                assert yaml.safe_load(f) == {}
        finally:
            os.remove(config_filepath)

    def test_update_closest(self, expected_config):
        cwd_config_filepath = os.path.abspath(_config_utils.CONFIG_YAML_FILENAME)
        new_key = 'NEW_KEY'
        new_value = "NEW_VALUE"

        # config in cwd does not yet have new item
        with open(cwd_config_filepath, 'r') as f:
            nearest_config = yaml.safe_load(f)
            assert new_key not in nearest_config

        with _config_utils.write_local_config() as config:
            config[new_key] = new_value

        # config in cwd updated with new item
        with open(cwd_config_filepath, 'r') as f:
            nearest_config = yaml.safe_load(f)
            assert new_key in nearest_config
            assert nearest_config[new_key] == new_value

        # other configs not updated with new item
        config_filepaths = _config_utils.find_config_files()  # util tested in TestRead
        config_filepaths = set(config_filepaths)
        config_filepaths.remove(cwd_config_filepath)  # don't count cwd config
        for config_filepath in config_filepaths:
            with open(config_filepath, 'r') as f:
                config = yaml.safe_load(f)
                assert new_key not in config

    def test_does_not_update_home(self, in_tempdir):
        home_verta_dir = _config_utils.HOME_VERTA_DIR
        config_filename = _config_utils.CONFIG_YAML_FILENAME

        # backup home config
        backup_dir = tempfile.mkdtemp(dir=".")
        if os.path.exists(os.path.join(home_verta_dir, config_filename)):
            shutil.move(
                os.path.join(home_verta_dir, config_filename),
                os.path.join(backup_dir, config_filename),
            )

        try:
            # create config in home dir
            home_config_filepath = _config_utils.create_empty_config_file(home_verta_dir)

            # nearest config is home
            assert _config_utils.find_closest_config_file() == home_config_filepath

            # do the thing we're actually testing
            with _config_utils.write_local_config() as config:
                config['host'] = "app.verta.ai"

            # config file created and written in cwd
            config = _config_utils.load(config_filename)
            assert config['host'] == "app.verta.ai"

            # home config unchanged
            config = _config_utils.load(home_config_filepath)
            assert 'host' not in config
        finally:
            # delete created home config
            os.remove(home_config_filepath)

            # restore home config
            if os.path.exists(os.path.join(backup_dir, config_filename)):
                shutil.move(
                    os.path.join(backup_dir, config_filename),
                    os.path.join(home_verta_dir, config_filename),
                )
