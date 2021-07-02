# -*- coding: utf-8 -*-

from __future__ import print_function

import importlib


def maybe_dependency(module_name):
    try:
        return importlib.import_module(module_name)
    except ImportError:
        return None


def get_tensorflow_major_version():
    tf = maybe_dependency("tensorflow")
    if tf:
        return int(tf.__version__.split('.')[0])
    return None
