# -*- coding: utf-8 -*-

import pytest

from verta.environment import Python
from verta._internal_utils._utils import generate_default_name
from verta.monitoring.profiler import ContinuousHistogramProfiler
from verta.monitoring.profilers import ProfilerReference
import pytest


class TestProfilers(object):

    def test_profiler_crud(self, client):
        requirements = ["numpy", "scipy", "pandas"]
        for req in requirements:
            pytest.importorskip(req)

        profilers = client.monitoring.profilers

        profiler_name = "age_column_profiler_{}".format(generate_default_name())
        python_env = Python(requirements=requirements)

        created_profiler = profilers.upload(profiler_name, ContinuousHistogramProfiler(columns=["age"]), environment=python_env)
        assert isinstance(created_profiler, ProfilerReference)

        retrieved_profiler = profilers.get(created_profiler.id)
        assert isinstance(retrieved_profiler, ProfilerReference)
        assert created_profiler.id == retrieved_profiler.id

        listed_profilers = profilers.list()
        assert len(listed_profilers) > 1
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
