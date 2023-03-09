# -*- coding: utf-8 -*-

import importlib
import logging
import os
import pkgutil
from types import ModuleType
from typing import List, Set

logger = logging.getLogger(__name__)


class CustomModules(object):
    @staticmethod
    def is_importable(module_name: str) -> bool:
        """Return whether `module_name` can be imported."""
        try:
            return True if pkgutil.find_loader(module_name) else False
        except ImportError:
            return False

    @staticmethod
    def get_module_path(module_name: str) -> str:
        """Return root path to `module`."""
        module_spec = importlib.util.find_spec(module_name)  # pylint: disable=no-member

        module_path = module_spec.submodule_search_locations  # module.__path__
        if module_path:  # directory-based package
            return module_path[0]  # root dir

        # single-file module
        return module_spec.origin  # module.__file__

    @staticmethod
    def categorize_custom_modules(custom_modules: List[str]):
        # TODO: docstring
        module_names, filepaths, dirpaths = set(), set(), set()
        for custom_module in custom_modules:
            if CustomModules.is_importable(custom_module):
                _bucket = module_names
            elif os.path.isfile(custom_module):
                _bucket = filepaths
            elif os.path.isdir(custom_module):
                _bucket = dirpaths
            else:
                raise ValueError(f"custom module {custom_module} not found")
            _bucket.add(custom_module)
        return module_names, filepaths, dirpaths

    @staticmethod
    def in_named_custom_modules(module: ModuleType, module_names: Set[str]) -> bool:
        """Return whether `module` is supplied by a named module."""
        if module.__name__ in module_names:
            logger.debug("found %s in modules", module)
            return True
        return False

    @staticmethod
    def in_file_custom_modules(module: ModuleType, filepaths: Set[str]) -> bool:
        """Return whether `module` is potentially supplied by a Python file within it."""
        for filepath in map(os.path.abspath, filepaths):
            if any(
                # TODO: check that all base modules have __path__
                map(filepath.startswith, module.__path__)
            ):
                logger.debug("found %s in filepaths", module)
                return True
        return False

    @staticmethod
    def in_directory_custom_modules(module: ModuleType, dirpaths: Set[str]) -> bool:
        """Return whether `module` is potentially supplied by a directory that contains it."""
        for dirpath in map(os.path.abspath, dirpaths):
            dirpath = dirpath.rstrip("/")
            if any(module_path.startswith(dirpath) for module_path in module.__path__):
                logger.debug("found %s in dirpaths", module)
                return True
        return False
