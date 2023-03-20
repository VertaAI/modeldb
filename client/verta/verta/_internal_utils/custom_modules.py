# -*- coding: utf-8 -*-

import importlib
import pkgutil


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
        """Return file/directory path to `module_name`."""
        module_spec = importlib.util.find_spec(module_name)  # pylint: disable=no-member

        module_path = module_spec.submodule_search_locations  # module.__path__
        if module_path:  # directory-based package
            return list(module_path)[0]  # directory (instead of its __init__.py)

        # single-file module
        return module_spec.origin  # module.__file__
