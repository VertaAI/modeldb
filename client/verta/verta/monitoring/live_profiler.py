# -*- coding: utf-8 -*-

import json

from verta._internal_utils._utils import proto_to_json
from verta.monitoring.profiler import ContinuousHistogramProfiler, BinaryHistogramProfiler
from verta.data_types import _VertaDataType
from verta.monitoring import profiler

from verta._protos.public.monitoring.Summary_pb2 import CreateSummarySample
from verta._protos.public.monitoring.DeploymentIntegration_pb2 import FeatureDataInProfiler


class FeatureProfiler(object):
    def __init__(self, feature_data):
        self.feature_data = feature_data
        # Error sources: no data type
        self.reference_content = _VertaDataType._from_dict(json.loads(feature_data.reference_content))

        profiler_name = feature_data.profiler_name
        profiler_args = json.loads(feature_data.profiler_parameters)
        # Error sources: profiler doesn't exist, or the arguments don't match
        self.profiler = getattr(profiler, profiler_name)(**profiler_args)

    def run(self, model_io):
        location = None
        if self.feature_data.feature_name in model_io["input"]:
            location = "input"
        elif self.feature_data.feature_name in model_io["output"]:
            location = "output"

        # Result doesn't exist either on input or output, so we skip
        if location is None:
            return None

        # Get the feature from either input or output
        data = model_io.get(location, {}).get(self.feature_data.feature_name, None)
        # Error sources: profiler fails to compute
        sample_value = self.profiler.profile_point(data, self.reference_content)

        if sample_value is None:
            return None

        create_summary_sample = CreateSummarySample(
            summary_id = self.feature_data.summary_id,
            labels = self.feature_data.labels,
            content = json.dumps(sample_value._as_dict()),
        )
        create_summary_sample.labels["io_type"] = location
        create_summary_sample.labels["feature"] = self.feature_data.feature_name

        return create_summary_sample


class LiveProfiler(object):
    def __init__(self, config_content):
        self.config_content = config_content
        self.feature_profilers = [FeatureProfiler(d) for d in self.config_content]

    def run(self, model_io):
        model_io = json.loads(model_io)

        response = []
        for p in self.feature_profilers:
            response.append(p.run(model_io))

        response = json.dumps([proto_to_json(r, include_default=False) for r in response if r is not None])
        return response


import pandas as pd


class TestLiveProfiler(object):
    def test_census(self):
        df = pd.read_csv("/Users/conrado/Downloads/census-train.csv")
        features = list(df.columns)
        continuous_inputs = {"age", "capital-gain", "capital-loss", "hours-per-week"}
        outputs = {">50k"}
        binary_inputs = set(features) - continuous_inputs - outputs

        features_data = []

        for id, f in enumerate(df.columns):
            if f in continuous_inputs:
                profiler = ContinuousHistogramProfiler(f)
                feature_data = FeatureDataInProfiler(
                    feature_name=f,
                    profiler_name="ContinuousHistogramProfiler",
                    profiler_parameters=json.dumps(dict(columns=f)),
                    summary_id=id,
                    labels={"foo":"bar", "io_type": "input", "feature": f},
                    reference_content="",
                )
            elif f in binary_inputs:
                profiler = BinaryHistogramProfiler(f)
                feature_data = FeatureDataInProfiler(
                    feature_name=f,
                    profiler_name="BinaryHistogramProfiler",
                    profiler_parameters=json.dumps(dict(columns=f)),
                    summary_id=id,
                    labels={"foo":"bar", "io_type": "input", "feature": f},
                    reference_content="",
                )
            else:
                profiler = BinaryHistogramProfiler(f)
                feature_data = FeatureDataInProfiler(
                    feature_name=f,
                    profiler_name="BinaryHistogramProfiler",
                    profiler_parameters=json.dumps(dict(columns=f)),
                    summary_id=id,
                    labels={"foo":"bar", "io_type": "output", "feature": f},
                    reference_content="",
                )

            reference = profiler.profile_column(df, f)[1]
            feature_data.reference_content = json.dumps(reference._as_dict())

            features_data.append(feature_data)

        live_profiler = LiveProfiler(features_data)
        model_io = {
            "input": {f: 0 for f in features if f not in outputs},
            "output": {f: 0 for f in outputs},
        }

        model_io = json.dumps(model_io)
        res = live_profiler.run(model_io)
        res = json.loads(res)

        print(json.dumps(res, indent=4, sort_keys=True))

        import timeit
        # 45ms
        print(timeit.timeit(lambda: live_profiler.run(model_io), number=100))