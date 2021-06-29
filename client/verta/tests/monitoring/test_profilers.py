# -*- coding: utf-8 -*-

import pytest
import hypothesis

from verta import data_types
from verta.environment import Python
from verta._internal_utils._utils import generate_default_name
from verta.monitoring.profiler import (
    BinaryHistogramProfiler,
    ContinuousHistogramProfiler,
    MissingValuesProfiler,
)
from verta.monitoring._profilers import ProfilerReference

from . import strategies


class TestProfilers:
    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_continuous(self, df):
        np = pytest.importorskip("numpy")

        cols = ["continuous"]

        profiler = ContinuousHistogramProfiler(cols)
        profile = profiler.profile(df)

        assert len(profile) == len(cols)
        for name, data in profile.items():
            assert isinstance(data, data_types.FloatHistogram)

            series = df[name.split("_histogram")[0]]
            values, limits = np.histogram(series.dropna(), bins=data._bucket_limits)
            assert set(data._bucket_limits) == set(limits.tolist())
            assert set(data._data) == set(values.tolist())
            # missing values omitted
            assert sum(data._data) == sum(~series.isna())

    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_discrete(self, df):
        cols = ["discrete"]

        profiler = BinaryHistogramProfiler(cols)
        profile = profiler.profile(df)

        assert len(profile) == len(cols)
        for name, data in profile.items():
            assert isinstance(data, data_types.DiscreteHistogram)

            series = df[name.split("_histogram")[0]]
            value_counts = series.value_counts()
            assert set(data._buckets) == set(value_counts.index)
            assert set(data._data) == set(value_counts.values)
            # missing values omitted
            assert sum(data._data) == sum(~series.isna())

    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_missing(self, df):
        cols = ["continuous", "discrete"]

        profiler = MissingValuesProfiler(cols)
        profile = profiler.profile(df)

        assert len(profile) == len(cols)
        for name, data in profile.items():
            assert isinstance(data, data_types.DiscreteHistogram)

            series = df[name.split("_missing")[0]]
            assert data._data[data._buckets.index("present")] == sum(~series.isna())
            assert data._data[data._buckets.index("missing")] == sum(series.isna())


class TestLiveProfilers(object):
    def test_profiler_crud(self, client):
        requirements = ["numpy", "scipy", "pandas"]
        for req in requirements:
            pytest.importorskip(req)

        profilers = client.monitoring._profilers

        profiler_name = "age_column_profiler_{}".format(generate_default_name())
        python_env = Python(requirements=requirements)

        created_profiler = profilers.upload(profiler_name, ContinuousHistogramProfiler(columns=["age"]), environment=python_env)
        assert isinstance(created_profiler, ProfilerReference)

        retrieved_profiler = profilers.get(created_profiler.id)
        assert isinstance(retrieved_profiler, ProfilerReference)
        assert created_profiler.id == retrieved_profiler.id

        listed_profilers = profilers.list()
        assert len(listed_profilers) >= 1
        assert created_profiler.id in map(lambda p: p.id, listed_profilers)

        old_name = created_profiler.name
        old_profiler_version = created_profiler.reference
        new_name = "profiler2_{}".format(generate_default_name())

        created_profiler.update(new_name)

        assert created_profiler.name == new_name
        assert created_profiler.name != old_name
        assert old_profiler_version == created_profiler.reference

        delete = profilers.delete(created_profiler)
        assert delete

    @pytest.mark.skip(reason="heavy remote operation, plan TBD")
    def test_deploy(self, client):
        pass
