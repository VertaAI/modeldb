# TODO: [MV] are these tests even necessary?
def validate_fit_event_struct(fitEvent, tester):
    tester.assertTrue(hasattr(fitEvent, 'df'))
    tester.assertTrue(hasattr(fitEvent, 'spec'))
    tester.assertTrue(hasattr(fitEvent, 'model'))
    tester.assertTrue(hasattr(fitEvent, 'featureColumns'))
    tester.assertTrue(hasattr(fitEvent, 'predictionColumns'))
    tester.assertTrue(hasattr(fitEvent, 'labelColumns'))
    tester.assertTrue(hasattr(fitEvent, 'experimentRunId'))
    tester.assertTrue(type(fitEvent.experimentRunId), 'int')

def validate_project_struct(project, tester):
    tester.assertTrue(hasattr(project, 'id')) 
    tester.assertTrue(hasattr(project, 'name'))
    tester.assertTrue(hasattr(project, 'author'))
    tester.assertTrue(hasattr(project, 'description'))
    tester.assertNotEqual(project.id, -1)

def validate_experiment_struct(experiment, tester):
    tester.assertTrue(hasattr(experiment, 'projectId'))
    tester.assertTrue(hasattr(experiment, 'description'))
    tester.assertTrue(hasattr(experiment, 'id'))
    tester.assertTrue(hasattr(experiment, 'isDefault'))
    tester.assertTrue(hasattr(experiment, 'name'))

def valdiate_experiment_run_struct(experimentRun, tester):
    tester.assertTrue(hasattr(experimentRun, 'id'))
    tester.assertTrue(hasattr(experimentRun, 'experimentId'))
    tester.assertTrue(hasattr(experimentRun, 'description'))

def validate_transformer_spec_struc(spec, tester):
    tester.assertTrue(hasattr(spec, 'id'))
    tester.assertTrue(hasattr(spec, 'transformerType')) 
    tester.assertTrue(hasattr(spec, 'features'))
    tester.assertTrue(hasattr(spec, 'hyperparameters'))
    tester.assertTrue(hasattr(spec, 'tag'))

def validate_transform_event_struct(transformEvent, tester):
    tester.assertTrue(hasattr(transformEvent, 'oldDataFrame'))
    tester.assertTrue(hasattr(transformEvent, 'newDataFrame'))
    tester.assertTrue(hasattr(transformEvent, 'transformer'))
    tester.assertTrue(hasattr(transformEvent, 'inputColumns'))
    tester.assertTrue(hasattr(transformEvent, 'outputColumns'))
    tester.assertTrue(hasattr(transformEvent, 'experimentRunId'))
    tester.assertTrue(type(transformEvent.experimentRunId), 'int')

def validate_dataframe_struct(dataframe, tester):
    tester.assertTrue(hasattr(dataframe, 'numRows'))
    tester.assertTrue(hasattr(dataframe, 'tag'))
    tester.assertTrue(hasattr(dataframe, 'id'))
    tester.assertTrue(hasattr(dataframe, 'schema'))

def validate_transformer_struct(transformer, tester):
    tester.assertTrue(hasattr(transformer, 'id'))
    tester.assertTrue(hasattr(transformer, 'weights')) 
    tester.assertTrue(hasattr(transformer, 'transformerType'))
    tester.assertTrue(hasattr(transformer, 'tag'))

def is_equal_dataframe(dataframe1, dataframe2, tester):
    tester.assertEqual(dataframe1.numRows, dataframe2.numRows)
    tester.assertEqual(dataframe1.tag, dataframe2.tag)
    tester.assertEqual(dataframe1.id, dataframe2.id)
    tester.assertEqual(len(dataframe1.schema), len(dataframe2.schema))

    # check schema
    for i in range(len(dataframe1.schema)):
        tester.assertEqual(dataframe1.schema[i].name, dataframe2.schema[i].name)
        tester.assertEqual(dataframe1.schema[i].type, dataframe2.schema[i].type)
    
def is_equal_transformer_spec(spec1, spec2, tester):
    tester.assertEqual(spec1.id, spec2.id)
    tester.assertEqual(spec1.transformerType, spec2.transformerType)
    tester.assertEqual(spec1.features, spec2.features)
    tester.assertEqual(spec1.tag, spec2.tag)

    tester.assertEqual(len(spec1.hyperparameters), len(spec2.hyperparameters))

    for i in range(len(spec1.hyperparameters)):
        tester.assertEqual(spec1.hyperparameters[i].name, 
            spec2.hyperparameters[i].name)
        tester.assertEqual(spec1.hyperparameters[i].value, 
            spec2.hyperparameters[i].value)
        tester.assertEqual(spec1.hyperparameters[i].type, 
            spec2.hyperparameters[i].type)

def is_equal_transformer(model1, model2, tester):
    tester.assertEqual(model1.id, model2.id)
    tester.assertEqual(model1.transformerType, model2.transformerType)
    tester.assertEqual(model1.weights, model2.weights)
    tester.assertEqual(model1.tag, model2.tag)

# self.assertEqual(project.id, experiment.projectId)
# self.assertEqual(experimentRun.experimentId, experiment.id)

# self.assertTrue(type(self.fitEvent.featureColumns), 'list')
# self.assertTrue(type(self.fitEvent.predictionColumns), 'list')
# self.assertTrue(type(self.fitEvent.labelColumns), 'list')
               