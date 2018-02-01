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


def validate_experiment_struct(experiment, tester):
    tester.assertTrue(hasattr(experiment, 'projectId'))
    tester.assertTrue(hasattr(experiment, 'description'))
    tester.assertTrue(hasattr(experiment, 'id'))
    tester.assertTrue(hasattr(experiment, 'isDefault'))
    tester.assertTrue(hasattr(experiment, 'name'))


def validate_experiment_run_struct(experiment_run, tester):
    tester.assertTrue(hasattr(experiment_run, 'id'))
    tester.assertTrue(hasattr(experiment_run, 'experimentId'))
    tester.assertTrue(hasattr(experiment_run, 'description'))


def validate_transformer_spec_struct(spec, tester):
    tester.assertTrue(hasattr(spec, 'id'))
    tester.assertTrue(hasattr(spec, 'transformerType'))
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
    tester.assertTrue(hasattr(transformer, 'transformerType'))
    tester.assertTrue(hasattr(transformer, 'tag'))


def validate_pipeline_event_struct(pipelineEvent, tester):
    tester.assertTrue(hasattr(pipelineEvent, 'pipelineFit'))
    tester.assertTrue(hasattr(pipelineEvent, 'transformStages'))
    tester.assertTrue(hasattr(pipelineEvent, 'fitStages'))
    tester.assertTrue(hasattr(pipelineEvent, 'experimentRunId'))


def validate_pipeline_fit_stages(fitStages, tester):
    count = 0
    for stage in fitStages:
        tester.assertTrue(hasattr(stage, 'fe'))
        tester.assertTrue(hasattr(stage, 'stageNumber'))
        tester.assertEqual(stage.stageNumber, count)
        validate_fit_event_struct(stage.fe, tester)
        count += 1


def validate_pipeline_transform_stages(transformStages, tester):
    count = 0
    for stage in transformStages:
        tester.assertTrue(hasattr(stage, 'te'))
        tester.assertTrue(hasattr(stage, 'stageNumber'))
        tester.assertEqual(stage.stageNumber, count)
        validate_transform_event_struct(stage.te, tester)
        count += 1


def validate_random_split_event_struct(random_splitEvent, tester):
    tester.assertTrue(hasattr(random_splitEvent, 'oldDataFrame'))
    tester.assertTrue(hasattr(random_splitEvent, 'weights'))
    tester.assertTrue(hasattr(random_splitEvent, 'seed'))
    tester.assertTrue(hasattr(random_splitEvent, 'splitDataFrames'))
    tester.assertTrue(hasattr(random_splitEvent, 'experimentRunId'))


def validate_metric_event_struct(metric_event, tester):
    tester.assertTrue(hasattr(metric_event, 'df'))
    tester.assertTrue(hasattr(metric_event, 'model'))
    tester.assertTrue(hasattr(metric_event, 'metricType'))
    tester.assertTrue(hasattr(metric_event, 'metricValue'))
    tester.assertTrue(hasattr(metric_event, 'labelCol'))
    tester.assertTrue(hasattr(metric_event, 'predictionCol'))
    tester.assertTrue(hasattr(metric_event, 'experimentRunId'))


def validate_grid_search_cv_event(gridcvEvent, tester):
    tester.assertTrue(hasattr(gridcvEvent, 'numFolds'))
    tester.assertTrue(hasattr(gridcvEvent, 'bestFit'))
    tester.assertTrue(hasattr(gridcvEvent, 'crossValidations'))
    tester.assertTrue(hasattr(gridcvEvent, 'experimentRunId'))


def validate_cross_validate_event(cvEvent, tester):
    tester.assertTrue(hasattr(cvEvent, 'df'))
    tester.assertTrue(hasattr(cvEvent, 'spec'))
    tester.assertTrue(hasattr(cvEvent, 'seed'))
    tester.assertTrue(hasattr(cvEvent, 'evaluator'))
    tester.assertTrue(hasattr(cvEvent, 'labelColumns'))
    tester.assertTrue(hasattr(cvEvent, 'predictionColumns'))
    tester.assertTrue(hasattr(cvEvent, 'featureColumns'))
    tester.assertTrue(hasattr(cvEvent, 'folds'))
    tester.assertTrue(hasattr(cvEvent, 'experimentRunId'))


def validate_cross_validation_fold(cvFold, tester):
    tester.assertTrue(hasattr(cvFold, 'model'))
    tester.assertTrue(hasattr(cvFold, 'validationDf'))
    tester.assertTrue(hasattr(cvFold, 'trainingDf'))
    tester.assertTrue(hasattr(cvFold, 'score'))


def is_equal_dataframe(dataframe1, dataframe2, tester):
    tester.assertEqual(dataframe1.numRows, dataframe2.numRows)
    tester.assertEqual(dataframe1.tag, dataframe2.tag)
    tester.assertEqual(dataframe1.id, dataframe2.id)
    tester.assertEqual(len(dataframe1.schema), len(dataframe2.schema))

    # check schema
    for i in range(len(dataframe1.schema)):
        tester.assertEqual(dataframe1.schema[
                           i].name, dataframe2.schema[i].name)
        tester.assertEqual(dataframe1.schema[
                           i].type, dataframe2.schema[i].type)


def is_equal_transformer_spec(spec1, spec2, tester):
    tester.assertEqual(spec1.id, spec2.id)
    tester.assertEqual(spec1.transformerType, spec2.transformerType)
    tester.assertEqual(spec1.tag, spec2.tag)

    tester.assertEqual(len(spec1.hyperparameters), len(spec2.hyperparameters))

    for i in range(len(spec1.hyperparameters)):
        tester.assertTrue(spec1.hyperparameters[i] in spec2.hyperparameters)


def is_equal_transformer(model1, model2, tester):
    tester.assertEqual(model1.id, model2.id)
    tester.assertEqual(model1.transformerType, model2.transformerType)
    tester.assertEqual(model1.tag, model2.tag)


def is_equal_project(project1, project2, tester):
    tester.assertEqual(project1.id, project2.id)
    tester.assertEqual(project1.name, project2.name)
    tester.assertEqual(project1.author, project2.author)
    tester.assertEqual(project1.description, project2.description)


def is_equal_experiment(experiment1, experiment2, tester):
    tester.assertEqual(experiment1.id, experiment2.id)
    tester.assertEqual(experiment1.projectId, experiment2.projectId)
    tester.assertEqual(experiment1.name, experiment2.name)
    tester.assertEqual(experiment1.description, experiment2.description)
    tester.assertEqual(experiment1.isDefault, experiment2.isDefault)


def is_equal_experiment_run(expRun1, expRun2, tester):
    tester.assertEqual(expRun1.id, expRun2.id)
    tester.assertEqual(expRun1.experimentId, expRun2.experimentId)
    tester.assertEqual(expRun1.description, expRun2.description)
