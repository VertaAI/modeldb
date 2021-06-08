# -*- coding: utf-8 -*-

import hypothesis.strategies as st  # import builds, sampled_from,
import pytest
from hypothesis import given, settings
import numpy as np  # TODO: move to importer.maybe_dependency
from verta.data_types import NumericValue


class TestNumericDistance:
    @settings(deadline=None)
    @given(x=st.floats(allow_nan=False))
    def test_identity(self, x):
        n_x = NumericValue(x)
        assert np.isclose(0, n_x.dist(n_x))

    @settings(deadline=None)
    @given(x=st.floats(allow_nan=False), y=st.floats(allow_nan=False))
    def test_discernibles(self, x, y):
        n_x = NumericValue(x)
        n_y = NumericValue(y)
        if not np.isclose(x, y):
            assert not np.isclose(0, n_x.dist(n_y))
        else:
            pass

    @settings(deadline=None)
    @given(
        x=st.floats(allow_nan=False, allow_infinity=False),
        y=st.floats(allow_nan=False, allow_infinity=False),
    )
    def test_symmetry(self, x, y):
        n_x = NumericValue(x)
        n_y = NumericValue(y)
        assert np.isclose(n_x.dist(n_y), n_y.dist(n_x))

    @settings(deadline=None)
    @given(
        x=st.floats(allow_nan=False, allow_infinity=False),
        y=st.floats(allow_nan=False, allow_infinity=False),
        z=st.floats(allow_nan=False, allow_infinity=False),
    )
    def test_triangle_inequality(self, x, y, z):
        n_x = NumericValue(x)
        n_y = NumericValue(y)
        n_z = NumericValue(z)
        d_xy = n_x.dist(n_y)
        d_xz = n_x.dist(n_z)
        d_zy = n_z.dist(n_y)
        assert d_xy <= (d_xz + d_zy)
