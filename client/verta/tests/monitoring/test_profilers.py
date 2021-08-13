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


class TestContinuousHistogramProfiler:
    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.simple_dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_profile(self, df):
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

    def test_profile_point(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        colname = "data"
        df = pd.DataFrame(
            {
                colname: np.r_[
                    0,
                    np.abs(np.random.normal(loc=10, scale=5, size=50)),
                    None,
                ],
            }
        )

        profiler = ContinuousHistogramProfiler([colname])
        reference = profiler.profile(df)[colname + "_histogram"]

        for value in df[colname].dropna():
            profile = profiler.profile_point(value, reference)
            assert profile._bucket_limits == reference._bucket_limits
            assert sum(profile._data) == 1

            # check value is in correct bucket
            lower_bucket_limit = max(
                filter(
                    lambda bucket: bucket <= value,
                    profile._bucket_limits,
                )
            )
            data_index = min(
                profile._bucket_limits.index(lower_bucket_limit),
                len(profile._data) - 1,  # right-most bucket
            )
            assert profile._data[data_index] == 1

        # value not in reference
        value = -5
        profile = profiler.profile_point(value, reference)
        assert profile._bucket_limits == reference._bucket_limits
        assert sum(profile._data) == 0

        # missing value
        value = None
        profile = profiler.profile_point(value, reference)
        assert profile._bucket_limits == reference._bucket_limits
        assert sum(profile._data) == 0


class TestBinaryHistogramProfiler:
    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.simple_dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_profile(self, df):
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

    def test_profile_point(self):
        pd = pytest.importorskip("pandas")
        colname = "data"
        df = pd.DataFrame({colname: [0, 1, None]})

        profiler = BinaryHistogramProfiler([colname])
        reference = profiler.profile(df)[colname + "_histogram"]

        for value in [0, 1]:  # 0 (when in reference) should not be discarded
            profile = profiler.profile_point(value, reference)
            assert profile._buckets == reference._buckets
            assert sum(profile._data) == 1
            assert profile._data[profile._buckets.index(value)] == 1

        # value not in reference
        value = 2
        profile = profiler.profile_point(value, reference)
        assert profile._buckets == reference._buckets
        assert sum(profile._data) == 0

        # missing value
        value = None
        profile = profiler.profile_point(value, reference)
        assert profile._buckets == reference._buckets
        assert sum(profile._data) == 0


class TestMissingValuesProfiler:
    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.simple_dataframes(),  # pylint: disable=no-value-for-parameter
    )
    def test_profile(self, df):
        cols = ["continuous", "discrete"]

        profiler = MissingValuesProfiler(cols)

        profile = profiler.profile(df)
        assert len(profile) == len(cols)
        for name, data in profile.items():
            assert isinstance(data, data_types.DiscreteHistogram)

            series = df[name.split("_missing")[0]]
            assert data._data[data._buckets.index("present")] == sum(~series.isna())
            assert data._data[data._buckets.index("missing")] == sum(series.isna())

    def test_profile_point(self):
        pd = pytest.importorskip("pandas")
        colname = "data"
        df = pd.DataFrame({colname: [0, 1, None]})

        profiler = MissingValuesProfiler([colname])
        reference = profiler.profile(df)[colname + "_missing"]

        for value in [0, 1, 2]:
            profile = profiler.profile_point(value, reference)
            assert profile._buckets == reference._buckets
            assert profile._data[profile._buckets.index("present")] == 1
            assert profile._data[profile._buckets.index("missing")] == 0

        # missing value
        value = None
        profile = profiler.profile_point(value, reference)
        assert profile._buckets == reference._buckets
        assert profile._data[profile._buckets.index("present")] == 0
        assert profile._data[profile._buckets.index("missing")] == 1


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
