# -*- coding: utf-8 -*-

import importlib
import textwrap
from types import FunctionType, ModuleType
import uuid

import pytest


@pytest.fixture
def custom_module_factory(monkeypatch, tmp_path_factory) -> FunctionType:
    TMP_PATH_BASENAME = "custom-module"
    CUSTOM_MODULE_CONTENTS_TMPL = textwrap.dedent(
        """
        # -*- coding: utf-8 -*-

        _VALUE = "{}"

        def get_value() -> str:
            return _VALUE
        """
    )

    def make_custom_module(import_path: str) -> ModuleType:
        """Create and return a custom Python module on local disk.

        The custom module simply contains two members:

        - ``_VALUE``: a string UUID
        - ``get_value()``: a function that takes no arguments and returns ``_VALUE``

        Parameters
        ----------
        import_path : str
            Dot-delimited path for the desired module, e.g. ``foo.bar.baz``.

        Returns
        -------
        ModuleType
            Module object. As with all modules, its ``__name__`` attribute is
            its full name (i.e. `import_path`), and ``__file__`` is its
            filepath.

        """
        # create base directory to contain custom module and add to sys.path
        base_dirpath = tmp_path_factory.mktemp(TMP_PATH_BASENAME)
        monkeypatch.syspath_prepend(str(base_dirpath))

        # determine custom module filepath
        *parent_modules_names, leaf_module_name = import_path.split(".")
        parent_module_dirpath = base_dirpath.joinpath(*parent_modules_names)
        module_filepath = parent_module_dirpath / (leaf_module_name + ".py")

        # create custom module on disk
        parent_module_dirpath.mkdir(parents=True, exist_ok=True)
        with open(str(module_filepath), "w") as f:
            f.write(CUSTOM_MODULE_CONTENTS_TMPL.format(str(uuid.uuid4())))

        return importlib.import_module(import_path)

    return make_custom_module
