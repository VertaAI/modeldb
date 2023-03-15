# -*- coding: utf-8 -*-

import importlib
from pathlib import Path
import textwrap
from types import FunctionType, ModuleType
import uuid

import pytest


@pytest.fixture
def make_custom_module(monkeypatch, tmp_path_factory) -> FunctionType:
    """Factory fixture for creating custom modules.

    See nested function's docstring for usage details.

    """
    monkeypatch.syspath_prepend(tmp_path_factory.getbasetemp())
    MODULE_BASENAME = "custom_module_"
    MODULE_CONTENTS_TMPL = textwrap.dedent(
        """
        # -*- coding: utf-8 -*-

        _VALUE = "{}"

        def get_value() -> str:
            return _VALUE
        """
    )

    def _make_custom_module(module_name: str) -> ModuleType:
        """Create and return a custom Python module on local disk.

        The custom module simply contains two members:

        - ``_VALUE``: a string UUID
        - ``get_value()``: a function that takes no arguments and returns ``_VALUE``

        Parameters
        ----------
        module_name : str
            Dot-delimited name for the desired module, e.g. ``foo.bar.baz``.
            For uniqueness, the actual custom module will nested in an
            arbitrary top-level parent, e.g. ``custom_module_1.foo.bar.baz``.

        Returns
        -------
        ModuleType
            Python module. As with all modules, its ``__name__`` attribute is
            its full name, and ``__file__`` is its filepath.

        """
        # create unique directory for top-level module
        root_module_dirpath: Path = tmp_path_factory.mktemp(MODULE_BASENAME)

        # determine custom module filepath
        *parent_modules_names, leaf_module_name = module_name.split(".")
        parent_module_dirpath: Path = root_module_dirpath.joinpath(
            *parent_modules_names,
        )
        module_filepath: Path = parent_module_dirpath / f"{leaf_module_name}.py"

        # create custom module on disk
        parent_module_dirpath.mkdir(parents=True, exist_ok=True)
        module_filepath.write_text(MODULE_CONTENTS_TMPL.format(str(uuid.uuid4())))

        importlib.invalidate_caches()
        return importlib.import_module(
            name=f".{module_name}",
            package=root_module_dirpath.name,
        )

    return _make_custom_module
