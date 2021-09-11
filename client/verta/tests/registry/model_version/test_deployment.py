# -*- coding: utf-8 -*-

import datetime
import filecmp
import glob
import json
import pickle
import os
import shutil
import sys
import tarfile
import tempfile
import uuid
import zipfile

import cloudpickle
import hypothesis
import hypothesis.strategies as st
import pytest
import six

from verta._protos.public.monitoring.DeploymentIntegration_pb2 import (
    FeatureDataInModelVersion,
)
from verta._internal_utils import _artifact_utils, _utils
from verta.data_types import _verta_data_type
from verta.endpoint.update import DirectUpdateStrategy
from verta.environment import Python
from verta.monitoring import profiler
from verta.registry.entities import RegisteredModelVersion
from verta.tracking.entities import _deployable_entity

from ... import utils
from ...monitoring import strategies


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestArbitraryModels:
    def test_from_run_download_arbitrary_directory(
        self,
        experiment_run,
        registered_model,
        dir_and_files,
        strs,
        in_tempdir,
    ):
        """Dir model logged to run is unpacked by model ver."""
        dirpath, _ = dir_and_files
        download_path = strs[0]

        experiment_run.log_model(dirpath)
        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )
        returned_path = model_version.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        utils.assert_dirs_match(dirpath, download_path)


class TestAutoMonitoring:
    @staticmethod
    def assert_feature_data_correctness(feature_data, in_df, out_df):
        """Verifies that profiler type and reference are correct for column."""
        col_type = feature_data.labels["col_type"]
        source_df = in_df if col_type == "input" else out_df
        assert feature_data.feature_name in source_df

        # reconstruct reference distribution
        reference_content = json.loads(feature_data.content)
        reference = _verta_data_type._VertaDataType._from_dict(reference_content)

        # reconstruct profiler
        profiler_name = feature_data.profiler_name
        profiler_args = json.loads(feature_data.profiler_parameters)
        feature_profiler = getattr(profiler, profiler_name)(**profiler_args)

        # verify re-profiling column yields reference distribution
        _, profile = feature_profiler.profile_column(
            source_df,
            feature_data.feature_name,
        )
        assert profile == reference

    def test_non_df(self, model_version):
        pd = pytest.importorskip("pandas")

        with pytest.raises(TypeError):
            model_version.log_training_data_profile(
                "abc",
                pd.DataFrame([1, 2, 3]),
            )
        with pytest.raises(TypeError):
            model_version.log_training_data_profile(
                pd.DataFrame([1, 2, 3]),
                2,
            )

        # coerce out_df if Series
        model_version.log_training_data_profile(
            pd.DataFrame([1, 2, 3], columns=["in"]),
            pd.Series([1, 2, 3], name="out"),
        )

    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.simple_dataframes(),  # pylint: disable=no-value-for-parameter
        labels=st.dictionaries(st.text(), st.text()),
    )
    def test_create_summaries(self, df, labels):
        """Unit test for the exact expected output of discrete & continuous columns."""
        pytest.importorskip("numpy")

        # missing
        for col in ["continuous", "discrete"]:
            feature_data = RegisteredModelVersion._create_missing_value_summary(
                df,
                col,
                labels,
            )
            _sample = profiler.MissingValuesProfiler([col]).profile(df)
            _histogram = list(_sample.values())[0]
            assert feature_data.feature_name == col
            assert feature_data.profiler_name == "MissingValuesProfiler"
            assert json.loads(feature_data.profiler_parameters) == {"columns": [col]}
            assert feature_data.summary_type_name == "verta.discreteHistogram.v1"
            assert feature_data.labels == labels
            assert json.loads(feature_data.content) == _histogram._as_dict()

        # continuous distribution
        feature_data = RegisteredModelVersion._create_continuous_histogram_summary(
            df,
            "continuous",
            labels,
        )
        _sample = profiler.ContinuousHistogramProfiler(["continuous"]).profile(df)
        _histogram = list(_sample.values())[0]
        assert feature_data.feature_name == "continuous"
        assert feature_data.profiler_name == "ContinuousHistogramProfiler"
        assert json.loads(feature_data.profiler_parameters) == {
            "columns": ["continuous"],
            "bins": _histogram._bucket_limits,
        }
        assert feature_data.summary_type_name == "verta.floatHistogram.v1"
        assert feature_data.labels == labels
        assert json.loads(feature_data.content) == _histogram._as_dict()

        # discrete distribution
        feature_data = RegisteredModelVersion._create_discrete_histogram_summary(
            df,
            "discrete",
            labels,
        )
        _sample = profiler.BinaryHistogramProfiler(["discrete"]).profile(df)
        _histogram = list(_sample.values())[0]
        assert feature_data.feature_name == "discrete"
        assert feature_data.profiler_name == "BinaryHistogramProfiler"
        assert json.loads(feature_data.profiler_parameters) == {"columns": ["discrete"]}
        assert feature_data.summary_type_name == "verta.discreteHistogram.v1"
        assert feature_data.labels == labels
        assert json.loads(feature_data.content) == _histogram._as_dict()

    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.dataframes(
            min_rows=1, min_cols=2
        ),  # pylint: disable=no-value-for-parameter
    )
    def test_compute_training_data_profile(self, df):
        """Unit test for helper functions handling DFs of various sizes."""
        in_df, out_df = df.iloc[:, :-1], df.iloc[:, [-1]]

        feature_data_list = RegisteredModelVersion._compute_training_data_profile(
            in_df,
            out_df,
        )
        for feature_data in feature_data_list:
            self.assert_feature_data_correctness(feature_data, in_df, out_df)

    @hypothesis.settings(deadline=None)  # building DataFrames can be slow
    @hypothesis.given(
        df=strategies.dataframes(
            min_rows=1, min_cols=2
        ),  # pylint: disable=no-value-for-parameter
    )
    def test_collect_feature_data_and_vis_attributes(self, df):
        """Unit test that attributes pre-logging are the correct format."""
        in_df, out_df = df.iloc[:, :-1], df.iloc[:, [-1]]

        feature_data_list = RegisteredModelVersion._compute_training_data_profile(
            in_df,
            out_df,
        )
        feature_data_attrs = (
            RegisteredModelVersion._collect_feature_data_and_vis_attributes(
                feature_data_list,
            )
        )

        for key, val in feature_data_attrs.items():
            if key.startswith(_deployable_entity._FEATURE_DATA_ATTR_PREFIX):
                feature_data = _utils.json_to_proto(val, FeatureDataInModelVersion)
                self.assert_feature_data_correctness(feature_data, in_df, out_df)

                if feature_data.profiler_name == "MissingValuesProfiler":
                    sample_key = feature_data.feature_name + "MissingValues"
                else:
                    sample_key = feature_data.feature_name + "Distribution"
                sample_key = (
                    _deployable_entity._TRAINING_DATA_ATTR_PREFIX
                    + RegisteredModelVersion._normalize_attribute_key(sample_key)
                )
                assert feature_data_attrs[sample_key] == json.loads(
                    feature_data.content
                )

    def test_profile_training_data(self, model_version):
        """Integration test for logging attributes with correct structure."""
        pd = pytest.importorskip("pandas")
        np = pytest.importorskip("numpy")

        cont_col = np.random.random(100)
        discrete_col = np.random.choice(5, 100)
        string_discrete_col = np.random.choice(["a", "b", "c", "d", "e"], size=100)
        string_freeform_col = [uuid.uuid4().hex.upper()[0:10] for _ in range(100)]
        other_col = [datetime.datetime.now() for x in range(100)]
        output_col = np.random.choice(2, 100)

        col_names = [
            "Continuous_Numeric",
            "Discrete_Numeric",
            "Discrete_String",
            "Freeform_String",
            "Other",
            "Output_Col",
        ]
        supported_col_names = ["Continuous_Numeric", "Discrete_Numeric", "Output_Col"]

        # create dataframes
        df = pd.DataFrame(
            list(
                zip(
                    cont_col,
                    discrete_col,
                    string_discrete_col,
                    string_freeform_col,
                    other_col,
                    output_col,
                )
            ),
            columns=col_names,
        )

        # log to model version with new method
        model_version.log_training_data_profile(
            df.loc[:, df.columns != "Output_Col"],
            pd.DataFrame(df["Output_Col"]),
        )

        # get back attributes to validate
        attributes = model_version.get_attributes()
        key = _deployable_entity._FEATURE_DATA_ATTR_PREFIX + "{}"
        discrete_col_missing_summary = _utils.json_to_proto(
            model_version.get_attribute(key.format("2")),
            FeatureDataInModelVersion,  # missing value
        )
        discrete_col_distribution_summary = _utils.json_to_proto(
            model_version.get_attribute(key.format("3")),
            FeatureDataInModelVersion,  # missing value
        )

        # missing value, distribution summary for each supported column +
        # equal number of attributes for visualization
        assert len(attributes.keys()) == len(supported_col_names) * 2 * 2
        assert (
            discrete_col_distribution_summary.summary_type_name
            == "verta.discreteHistogram.v1"
        )
        assert (
            discrete_col_distribution_summary.profiler_name == "BinaryHistogramProfiler"
        )
        assert (
            len(
                json.loads(discrete_col_distribution_summary.content)[
                    "discreteHistogram"
                ]["buckets"]
            )
            <= 5
        )

        assert (
            discrete_col_missing_summary.summary_type_name
            == "verta.discreteHistogram.v1"
        )
        assert discrete_col_missing_summary.profiler_name == "MissingValuesProfiler"
        assert (
            len(
                json.loads(discrete_col_missing_summary.content)["discreteHistogram"][
                    "buckets"
                ]
            )
            == 2
        )

        # reference distribution attributes can be fetched back as histograms
        for col in supported_col_names:
            key = _deployable_entity._TRAINING_DATA_ATTR_PREFIX + col + "Distribution"
            histogram = model_version.get_attribute(key)
            assert isinstance(histogram, _verta_data_type._VertaDataType)

    def test_reconstruct_profilers(self, model_version):
        """Profiler and ref distribution can be reconstructed from attr."""
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")

        in_col = "continuous"
        out_col = "discrete"
        num_rows = 24
        df = pd.DataFrame(
            {
                in_col: np.random.random(size=num_rows) * 10,
                out_col: range(num_rows),
            },
        )
        model_version.log_training_data_profile(
            in_df=df[[in_col]],
            out_df=df[[out_col]],
        )

        for key, val in model_version.get_attributes().items():
            if key.startswith(_deployable_entity._FEATURE_DATA_ATTR_PREFIX):
                feature_data = val

                reference_content = json.loads(feature_data["content"])
                reference = _verta_data_type._VertaDataType._from_dict(
                    reference_content
                )

                profiler_name = feature_data["profiler_name"]
                profiler_args = json.loads(feature_data["profiler_parameters"])
                feature_profiler = getattr(profiler, profiler_name)(**profiler_args)

                if isinstance(feature_profiler, profiler.MissingValuesProfiler):
                    point = None
                elif isinstance(feature_profiler, profiler.BinaryHistogramProfiler):
                    point = np.random.randint(num_rows)
                elif isinstance(feature_profiler, profiler.ContinuousHistogramProfiler):
                    point = np.random.random() * 10
                else:
                    raise TypeError(
                        "this test doesn't support profiler type {}".format(
                            type(feature_profiler)
                        )
                    )
                point_profile = feature_profiler.profile_point(point, reference)
                assert point_profile._type_string() == feature_data["summary_type_name"]
