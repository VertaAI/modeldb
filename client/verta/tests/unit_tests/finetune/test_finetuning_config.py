# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
from hypothesis import given
import pytest

from verta.finetune import _FinetuningConfig, LoraConfig


class TestFinetuningConfig:
    def test_init_subclasses(self):
        """Verify subclasses can be instantiated."""
        assert [subcls() for subcls in _FinetuningConfig.__subclasses__()]

    def test_from_job_dict_no_config_error(self):
        """Verify _from_job_dict() raises error for missing config."""
        with pytest.raises(ValueError, match="fine-tuning config not found"):
            _FinetuningConfig._from_job_dict(dict())


class TestLoraConfig:
    @hypothesis.given(
        include_alpha=st.booleans(),
        alpha=st.integers(max_value=0),
        include_dropout=st.booleans(),
        dropout=st.floats(),
        include_r=st.booleans(),
        r=st.integers(max_value=0),
    )
    def test_init_error(
        self,
        include_alpha,
        alpha,
        include_dropout,
        dropout,
        include_r,
        r,
    ):
        """Verify argument value validation."""
        hypothesis.assume(any([include_alpha, include_dropout, include_r]))
        if include_dropout:
            hypothesis.assume(dropout < 0.0 or dropout > 1.0)

        kwargs = dict()
        if include_alpha:
            kwargs["alpha"] = alpha
        if include_dropout:
            kwargs["dropout"] = dropout
        if include_r:
            kwargs["r"] = r

        with pytest.raises(ValueError, match="must be a"):
            LoraConfig(**kwargs)

    @given(
        alpha=st.integers(min_value=1),
        dropout=st.floats(min_value=0.0, max_value=1.0),
        r=st.integers(min_value=1),
    )
    def test_as_dict_conversion(self, alpha, dropout, r):
        """Verify _as_dict() has expected items."""
        config = LoraConfig(alpha=alpha, dropout=dropout, r=r)

        assert config._as_dict() == {
            "alpha": alpha,
            "dropout": dropout,
            "r": r,
        }

    @given(
        alpha=st.integers(min_value=1),
        dropout=st.floats(min_value=0.0, max_value=1.0),
        r=st.integers(min_value=1),
    )
    def test_from_job_dict(self, alpha, dropout, r):
        """Verify _from_job_dict() returns expected LoraConfig."""
        job_dict = {
            LoraConfig._JOB_DICT_KEY: {
                "alpha": alpha,
                "dropout": dropout,
                "r": r,
            }
        }

        assert _FinetuningConfig._from_job_dict(job_dict) == LoraConfig(
            alpha=alpha,
            dropout=dropout,
            r=r,
        )
