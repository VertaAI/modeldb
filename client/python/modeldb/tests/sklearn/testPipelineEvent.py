import unittest
import sys
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn import linear_model
from sklearn.pipeline import Pipeline
from sklearn import decomposition

import pandas as pd

FMIN = sys.float_info.min
FMAX = sys.float_info.max


class TestPipelineEvent(unittest.TestCase):

    @classmethod
    def setUp(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None))

        # Creating the pipeline
        pca = decomposition.PCA()
        lr = linear_model.LinearRegression()
        pipe = Pipeline(steps=[('pca', pca), ('logistic', lr)])
        model = linear_model.LinearRegression()
        np.random.seed(0)
        X = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 2)), columns=list('AB'))
        y = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 1)), columns=['output'])

        # Add tags for models / dataframes
        syncer_obj.add_tag(X, "digits-dataset")
        syncer_obj.add_tag(pipe, "pipeline with pca + logistic")
        syncer_obj.add_tag(pca, "decomposition PCA")
        syncer_obj.add_tag(lr, "basic linear reg")

        syncer_obj.clear_buffer()
        pipe.fit_sync(X, y)
        events = syncer_obj.sync()
        self.pipeline_event = events[0]

    def test_pipeline_construction(self):
        utils.validate_pipeline_event_struct(self.pipeline_event, self)

    def test_overall_pipeline_fit_event(self):
        fit_event = self.pipeline_event.pipelineFit
        utils.validate_fit_event_struct(fit_event, self)
        transformer = fit_event.model
        expected_transformer = modeldb_types.Transformer(
            -1,
            'Pipeline',
            'pipeline with pca + logistic')
        utils.is_equal_transformer(transformer, expected_transformer, self)

        df = fit_event.df
        expected_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('A', 'int64'),
                modeldb_types.DataFrameColumn('B', 'int64'),
            ],
            100,
            'digits-dataset')
        utils.is_equal_dataframe(df, expected_df, self)

        spec = fit_event.spec
        expected_spec = modeldb_types.TransformerSpec(
            -1,
            'Pipeline',
            [
                modeldb_types.HyperParameter(
                    'logistic__n_jobs', '1', 'int', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'pca__copy', 'True', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'pca__n_components', 'None', 'NoneType', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'logistic__fit_intercept', 'True', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'pca__whiten', 'False', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'steps', "[('pca', PCA(copy=True, n_components=None, whiten=False)), ('logistic', LinearRegression(copy_X=True, fit_intercept=True, n_jobs=1, normalize=False))]", 'list', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'logistic', 'LinearRegression(copy_X=True, fit_intercept=True, n_jobs=1, normalize=False)', 'LinearRegression', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'pca', 'PCA(copy=True, n_components=None, whiten=False)', 'PCA', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'logistic__normalize', 'False', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'logistic__copy_X', 'True', 'bool', FMIN, FMAX)
            ],
            'pipeline with pca + logistic')
        utils.is_equal_transformer_spec(spec, expected_spec, self)

        self.assertEqual(fit_event.featureColumns, ['A', 'B'])

    def test_pipeline_fit_stages(self):
        fit_stages = self.pipeline_event.fitStages
        utils.validate_pipeline_fit_stages(fit_stages, self)
        self.assertEqual(len(fit_stages), 2)

    def test_pipeline_first_fit_stage(self):
        fit_stages = self.pipeline_event.fitStages
        fit_event1 = fit_stages[0].fe
        # First Stage
        transformer = fit_event1.model
        expected_transformer = modeldb_types.Transformer(
            -1,
            'PCA',
            'decomposition PCA')
        utils.is_equal_transformer(transformer, expected_transformer, self)

        df = fit_event1.df
        expected_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('A', 'int64'),
                modeldb_types.DataFrameColumn('B', 'int64'),
            ],
            100,
            'digits-dataset')
        utils.is_equal_dataframe(df, expected_df, self)

        spec = fit_event1.spec
        expected_spec = modeldb_types.TransformerSpec(
            -1,
            'PCA',
            [
                modeldb_types.HyperParameter(
                    'copy', 'True', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'n_components', 'None', 'NoneType', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'whiten', 'False', 'bool', FMIN, FMAX),
            ],
            'decomposition PCA')
        utils.is_equal_transformer_spec(spec, expected_spec, self)

        self.assertEqual(fit_event1.featureColumns, ['A', 'B'])

    def test_pipeline_second_fit_stage(self):
        fit_stages = self.pipeline_event.fitStages
        fit_event2 = fit_stages[1].fe
        # Second Stage
        transformer = fit_event2.model
        expected_transformer = modeldb_types.Transformer(
            -1,
            'LinearRegression',
            'basic linear reg')
        utils.is_equal_transformer(transformer, expected_transformer, self)

        df = fit_event2.df
        expected_df = modeldb_types.DataFrame(
            -1,
            [],
            100,
            '')
        utils.is_equal_dataframe(df, expected_df, self)

        spec = fit_event2.spec
        expected_spec = modeldb_types.TransformerSpec(
            -1,
            'LinearRegression',
            [
                modeldb_types.HyperParameter(
                    'copy_X', 'True', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'normalize', 'False', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter('n_jobs', '1', 'int', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'fit_intercept', 'True', 'bool', FMIN, FMAX)
            ],
            'basic linear reg')
        utils.is_equal_transformer_spec(spec, expected_spec, self)

    def test_pipeline_transform_stages(self):
        transform_stages = self.pipeline_event.transformStages
        utils.validate_pipeline_transform_stages(transform_stages, self)
        self.assertEqual(len(transform_stages), 1)

    def test_pipeline_first_transform_stage(self):
        transform_stages = self.pipeline_event.transformStages
        transform_event = transform_stages[0].te

        transformer = transform_event.transformer
        expected_transformer = modeldb_types.Transformer(
            -1,
            'PCA',
            'decomposition PCA')
        utils.is_equal_transformer(transformer, expected_transformer, self)

        old_df = transform_event.oldDataFrame
        expected_old_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('A', 'int64'),
                modeldb_types.DataFrameColumn('B', 'int64'),
            ],
            100,
            'digits-dataset')
        utils.is_equal_dataframe(expected_old_df, old_df, self)

        new_df = transform_event.newDataFrame
        expected_new_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('0', 'float64'),
                modeldb_types.DataFrameColumn('1', 'float64'),
            ],
            100,
            '')
        utils.is_equal_dataframe(expected_new_df, new_df, self)


if __name__ == '__main__':
    unittest.main()
