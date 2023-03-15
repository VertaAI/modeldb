# -*- coding: utf-8 -*-

import importlib
import string
import textwrap
from types import FunctionType, ModuleType
import uuid

from hypothesis import strategies as st
import pytest


@st.composite
def custom_module_import_path(draw, max_depth=8):
    depth = draw(st.integers(min_value=0, max_value=max_depth))
    st.text(alphabet=string.ascii_lowercase + "_", min_size=1, max_size=32)
    raise NotImplementedError


@pytest.fixture
def custom_module_factory(monkeypatch, tmp_path_factory) -> FunctionType:
    TMP_PATH_BASENAME = "custom-module"
    CUSTOM_MODULE_CONTENTS_TMPL = textwrap.dedent(
        """
        # -*- coding: utf-8 -*-

        _VALUE = "{}"

        def get_value():
            return _VALUE
        """
    )

    def make_custom_module(import_path: str) -> ModuleType:
        """Create and return a custom Python module on local disk.

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
