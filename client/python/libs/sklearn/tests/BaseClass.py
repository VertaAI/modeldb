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
                transformer = self.fitEvent.model
                self.assertTrue(hasattr(transformer, 'id'))
                self.assertTrue(hasattr(transformer, 'weights')) 
                self.assertTrue(hasattr(transformer, 'transformerType'))
                self.assertTrue(hasattr(transformer, 'tag'))

                #Check DataFrame within FitEvent
                df = self.fitEvent.df
                self.assertTrue(hasattr(df, 'numRows'))
                self.assertTrue(hasattr(df, 'tag')) 
                self.assertTrue(hasattr(df, 'id'))
                self.assertTrue(hasattr(df, 'schema'))

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
                transformer = self.transformEvent.transformer
                self.assertTrue(hasattr(transformer, 'id'))
                self.assertTrue(hasattr(transformer, 'weights')) 
                self.assertTrue(hasattr(transformer, 'transformerType'))
                self.assertTrue(hasattr(transformer, 'tag'))

                #Check dataFrames within TransformEvent
                old_df = self.transformEvent.oldDataFrame
                new_df = self.transformEvent.newDataFrame
                for df in [old_df, new_df]:
                    self.assertTrue(hasattr(df, 'numRows'))
                    self.assertTrue(hasattr(df, 'tag')) 
                    self.assertTrue(hasattr(df, 'id'))
                    self.assertTrue(hasattr(df, 'schema'))
