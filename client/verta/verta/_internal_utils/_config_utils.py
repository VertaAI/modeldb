# -*- coding: utf-8 -*-

import json
import os

import yaml


# TODO: make this a named tuple, if it would help readability
CONFIG_YAML_FILENAME = "verta_config.yaml"
CONFIG_JSON_FILENAME = "verta_config.json"
CONFIG_FILENAMES = {
    CONFIG_YAML_FILENAME,
    CONFIG_JSON_FILENAME,
}


def create_empty_config_file(dirpath):
    """
    Creates ``verta_config.yaml`` containing an empty dictionary in `dirpath`.

    Parameters
    ----------
    dirpath : str
        Path to the directory that will contain the config file.

    Returns
    -------
    config_filepath : str
        Absolute path to the newly-created config file

    """
    config_filepath = os.path.join(dirpath, CONFIG_YAML_FILENAME)
    config_filepath = os.path.abspath(config_filepath)

    with open(config_filepath, 'w') as f:
        yaml.dump({}, f)

    return config_filepath
