import numpy as np

from patsy import dmatrices
import statsmodels.api as sm

import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model

from ModelDbSyncer import *
import SyncableRandomSplit 
import SyncableMetrics 
from ModelDbSyncerTest import *

import unittest

class BaseTestCases:
    class BaseClass(unittest.TestCase):

        # Checks if experiment, project, and experimentRun are populated properly.
        def test_session_creation(self):
            project = SyncerTest.instance.project
            experiment = SyncerTest.instance.experiment
            experimentRun = SyncerTest.instance.experimentRun

            # Check project attributes
            self.assertTrue(hasattr(project, 'id'))
            self.assertTrue(hasattr(project, 'name'))
            self.assertTrue(hasattr(project, 'author'))
            self.assertTrue(hasattr(project, 'description'))
            self.assertNotEqual(project.id, -1)

            # Check experiment attributes
            self.assertTrue(hasattr(experiment, 'projectId'))
            self.assertTrue(hasattr(experiment, 'description'))
            self.assertTrue(hasattr(experiment, 'id'))
            self.assertTrue(hasattr(experiment, 'isDefault'))
            self.assertTrue(hasattr(experiment, 'name'))

            # Check experimentRun attributes
            self.assertTrue(hasattr(experimentRun, 'id'))
            self.assertTrue(hasattr(experimentRun, 'experimentId'))
            self.assertTrue(hasattr(experimentRun, 'description'))

            #Check id dependencies
            self.assertEqual(project.id, experiment.projectId)
            self.assertEqual(experimentRun.experimentId, experiment.id)

        # Tests if all attributes present in FitEvent object.
        def test_fit_event_attributes(self):
            if (hasattr(self,  'fitEvent')):
                self.assertTrue(hasattr(self.fitEvent, 'df'))
                self.assertTrue(hasattr(self.fitEvent, 'spec'))
                self.assertTrue(hasattr(self.fitEvent, 'model'))
                self.assertTrue(hasattr(self.fitEvent, 'featureColumns'))
                self.assertTrue(hasattr(self.fitEvent, 'predictionColumns'))
                self.assertTrue(hasattr(self.fitEvent, 'labelColumns'))
                self.assertTrue(hasattr(self.fitEvent, 'experimentRunId'))
                self.assertTrue(type(self.fitEvent.experimentRunId), 'int')
                
                #Check TransformerSpec within FitEvent
                spec = self.fitEvent.spec
                self.assertTrue(hasattr(spec, 'id'))
                self.assertTrue(hasattr(spec, 'transformerType')) 
                self.assertTrue(hasattr(spec, 'features'))
                self.assertTrue(hasattr(spec, 'hyperparameters'))
                self.assertTrue(hasattr(spec, 'tag'))

                #Check Transformer within FitEvent
                self.test_transformer_struct()

                #Check DataFrame within FitEvent
                self.test_data_frame_struct()

                #Check types of columns
                self.assertTrue(type(self.fitEvent.featureColumns), 'list')
                self.assertTrue(type(self.fitEvent.predictionColumns), 'list')
                self.assertTrue(type(self.fitEvent.labelColumns), 'list')

        # Tests if all attributes present in TransformEvent object.
        def test_transform_event_attributes(self):
            if (hasattr(self,  'transformEvent')):
                self.assertTrue(hasattr(self.transformEvent, 'oldDataFrame'))
                self.assertTrue(hasattr(self.transformEvent, 'newDataFrame'))
                self.assertTrue(hasattr(self.transformEvent, 'transformer'))
                self.assertTrue(hasattr(self.transformEvent, 'inputColumns'))
                self.assertTrue(hasattr(self.transformEvent, 'outputColumns'))
                self.assertTrue(hasattr(self.transformEvent, 'experimentRunId'))
                self.assertTrue(type(self.transformEvent.experimentRunId), 'int')

                #Check Transformer within TransformEvent
                self.test_transformer_struct()

                #Check DataFrames within TransformEvent
                self.test_data_frame_struct()

        # Helper method to validate DataFrame struct
        def test_data_frame_struct(self):
            def check_df(df):
                self.assertTrue(hasattr(df, 'numRows'))
                self.assertTrue(hasattr(df, 'tag'))
                self.assertTrue(hasattr(df, 'id'))
                self.assertTrue(hasattr(df, 'schema'))
            if hasattr(self, 'transformEvent'):
                check_df(self.transformEvent.oldDataFrame)
                check_df(self.transformEvent.newDataFrame)
            if hasattr(self, 'fitEvent'):
                check_df(self.fitEvent.df)

        # Helper method to validate Transformer struct
        def test_transformer_struct(self):
            def check_struct(transformer):
                self.assertTrue(hasattr(transformer, 'id'))
                self.assertTrue(hasattr(transformer, 'weights')) 
                self.assertTrue(hasattr(transformer, 'transformerType'))
                self.assertTrue(hasattr(transformer, 'tag'))
            if hasattr(self, 'transformEvent'):
                check_struct(self.transformEvent.transformer)
            if hasattr(self, 'fitEvent'):
                check_struct(self.fitEvent.model)
