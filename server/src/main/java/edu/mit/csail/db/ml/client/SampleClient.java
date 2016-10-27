package edu.mit.csail.db.ml.client;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import modeldb.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SampleClient {
  /**
   * Test every method of the Thrift server.
   */
  private static void runAllTests(ModelDBService.Client client) throws Exception {
    testConnection(client);
    testProjectEvent(client);
    testExperimentEvent(client);
    testExperimentRunEvent(client);
    testTransformEvent(client);
    testAnnotationEvent(client);
    testFitEvent(client);
    testRandomSplitEvent(client);
    testMetricEvent(client);
    testPipelineEvent(client);
    testCrossValidationEvent(client);
    testGridSearchCrossValidationEvent(client);
    testDataFrameAncestry(client);
    testCommonAncestor(client);
    testNumRows(client);
    testHyperparameterComparison(client);
    testFeatureComparison(client);
    testGroupByProblemType(client);
    testSimilarModels(client);
    testLinearModel(client);
    testLinearModelFeatureImportances(client);
    testException(client);
    testLinearModelFeatureImportancesTwoModels(client);
    testIterationsUntilConvergence(client);
    testRankModels(client);
    testConfidenceIntervals(client);
    testModelsWithFeatures(client);
    testDescendentModels(client);
    testReadInfo(client);
    testReadRunsInExperiment(client);
    testReadRunsAndExperimentsInProject(client);
    testProjectOverviews(client);
    testExperimentRunDetails(client);
  }

  private static void testTransformEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeTransformEvent(StructFactory.makeTransformEvent()));
  }

  private static void testAnnotationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeAnnotationEvent(StructFactory.makeAnnotationEvent()));
  }

  private static void testFitEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeFitEvent(StructFactory.makeFitEvent()));
  }

  private static void testRandomSplitEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeRandomSplitEvent(StructFactory.makeRandomSplitEvent()));
  }

  private static void testMetricEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeMetricEvent(StructFactory.makeMetricEvent()));
  }

  private static void testProjectEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeProjectEvent(StructFactory.makeProjectEvent()));
  }

  private static void testExperimentEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeExperimentEvent(StructFactory.makeExperimentEvent()));
  }

  private static void testExperimentRunEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeExperimentRunEvent(StructFactory.makeExperimentRunEvent()));
  }

  private static void testPipelineEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storePipelineEvent(StructFactory.makePipelineEvent()));
  }

  private static void testCrossValidationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeCrossValidationEvent(StructFactory.makeCrossValidationEvent()));
  }

  private static void testGridSearchCrossValidationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeGridSearchCrossValidationEvent(StructFactory.makeGridSearchCrossValidationEvent()));
  }

  private static void testDataFrameAncestry(ModelDBService.Client client) throws Exception {
    for (int i = 0; i < 4; i++) {
      client.storeTransformEvent(StructFactory.makeTransformEvent());
    }
    System.out.println(client.getDataFrameAncestry(3));
  }

  private static void testCommonAncestor(ModelDBService.Client client) throws Exception {
    // Store a TransformEvent to ensure that we have a DataFrame with ID = 1.
    client.storeTransformEvent(StructFactory.makeTransformEvent());

    // Create another TransformEvent that creates a model that branches off DataFrame 1.
    TransformEvent te = StructFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(1));
    TransformEventResponse resp = client.storeTransformEvent(te);
    int commonAncestorId = resp.newDataFrameId;

    // Create two TransformEvents that branch off the DataFrame created above.
    te = StructFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(commonAncestorId));
    int dfId1 = client.storeTransformEvent(te).newDataFrameId;

    te = StructFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(commonAncestorId));
    int dfId2 = client.storeTransformEvent(te).newDataFrameId;

    // Now find the common ancestor of those two DataFrames, it should be the commonAncestorId.
    System.out.println(client.getCommonAncestor(dfId1, dfId2));

    // Now create FitEvents for dfId1 and dfId2.
    FitEvent fe = StructFactory.makeFitEvent();
    fe.setDf(fe.getDf().setId(dfId1));
    int modelId1 = client.storeFitEvent(fe).modelId;

    fe = StructFactory.makeFitEvent();
    fe.setDf(fe.getDf().setId(dfId2));
    int modelId2 = client.storeFitEvent(fe).modelId;

    // Find the common ancestor of the two models, it should be commonAncestorId.
    System.out.println(client.getCommonAncestorForModels(modelId1, modelId2));

    System.out.println("Common ancestor was " + commonAncestorId);
  }

  private static void testNumRows(ModelDBService.Client client) throws Exception {
    // Store some FitEvents.
    int NUM_FIT_EVENTS = 10;
    List<Integer> modelIds = new ArrayList<>();
    for (int i = 0; i < NUM_FIT_EVENTS; i++) {
      modelIds.add(client.storeFitEvent(StructFactory.makeFitEvent()).modelId);
    }

    // Now get the number of rows in each model.
    System.out.println(client.getTrainingRowsCount(modelIds.get(0)));
    System.out.println(client.getTrainingRowsCounts(modelIds));
  }

  private static void testHyperparameterComparison(ModelDBService.Client client) throws Exception {
    // Store two FitEvents.
    int model1Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int model2Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Compare the model's hyperparameters.
    System.out.println(client.compareHyperparameters(model1Id, model2Id));
  }

  private static void testFeatureComparison(ModelDBService.Client client) throws Exception {
    // Store two FitEvents.
    int model1Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int model2Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Compare the model's features.
    System.out.println(client.compareFeatures(model1Id, model2Id));
  }

  private static void testGroupByProblemType(ModelDBService.Client client) throws Exception {
    // Store some FitEvents.
    int NUM_EVENTS = 7;
    List<Integer> modelIds = new ArrayList<>();
    for (int i = 0; i < NUM_EVENTS; i++) {
      FitEvent fe = StructFactory.makeFitEvent();
      modelIds.add(client.storeFitEvent(fe).modelId);
      System.out.println("Type = " + fe.problemType.toString() + " and id = " + modelIds.get(modelIds.size() - 1));
    }

    // Now group.
    System.out.println(client.groupByProblemType(modelIds));
  }

  private static void testSimilarModels(ModelDBService.Client client) throws Exception {
    // Store some FitEvents.
    for (int i = 0; i < 50; i++) {
      client.storeFitEvent(StructFactory.makeFitEvent());
    }

    // Store another fit event and find the 3 models most similar to it.
    int modelId = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    System.out.println(
      client
        .similarModels(
          modelId,
          Arrays.asList(
            ModelCompMetric.MODEL_TYPE,
            ModelCompMetric.PROJECT,
            ModelCompMetric.PROBLEM_TYPE,
            ModelCompMetric.EXPERIMENT_RUN,
            ModelCompMetric.RMSE
          ),
          3
        )
        .stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(", "))
    );
  }

  private static void testLinearModel(ModelDBService.Client client) throws Exception {
    // Store a FitEvent so that we have Transformer with ID = 1 in the database.
    int modelId = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store a LinearModel.
    System.out.println(client.storeLinearModel(modelId, StructFactory.makeLinearModel()));
  }

  private static void testLinearModelFeatureImportances(ModelDBService.Client client) throws Exception {
    // Store a FitEvent so that we have Transformer with ID = 1 in the database.
    int modelId = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store a LinearModel.
    client.storeLinearModel(modelId, StructFactory.makeLinearModel());

    // Fetch the feature importances of the linear model.
    System.out.println(client.linearModelFeatureImportances(modelId));
  }

  private static void testException(ModelDBService.Client client) throws Exception {
    // Store a FitEvent so that we have Transformer with ID = 1 in the database.
    int modelId = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store a LinearModel.
    client.storeLinearModel(modelId, StructFactory.makeLinearModel());

    // Access a model ID that does not exist. This should thrown a ResourceNotFoundException.
    try {
      client.linearModelFeatureImportances(999999);
      System.out.println("ERROR: Did not throw an exception!");
    } catch (ResourceNotFoundException ex) {
      System.out.println("CORRECT: Properly through exception: " + ex.getMessage());
    } catch (Exception ex) {
      System.out.println("ERROR: Wrong exception type! " + ex.getClass().getSimpleName());
    }
  }

  private static void testLinearModelFeatureImportancesTwoModels(ModelDBService.Client client) throws Exception {
    // Store FitEvents so that we have Transformers with ID = 1 and ID = 2 in the database.
    int modelId1 = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int modelId2 = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    client.storeLinearModel(modelId1, StructFactory.makeLinearModel());
    client.storeLinearModel(modelId2, StructFactory.makeLinearModel());

    // Fetch feature importance comparison for them.
    System.out.println(client.compareLinearModelFeatureImportances(modelId1, modelId2));
  }

  private static void testIterationsUntilConvergence(ModelDBService.Client client) throws Exception {
    // Store a few models.
    int m1Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int m2Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int m3Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    client.storeLinearModel(m1Id, StructFactory.makeLinearModel());
    client.storeLinearModel(m2Id, StructFactory.makeLinearModel());
    client.storeLinearModel(m3Id, StructFactory.makeLinearModel());

    // Compute convergence time.
    System.out.println(
      client.iterationsUntilConvergence(Arrays.asList(m1Id, m2Id, m3Id), 0.5)
        .stream()
        .map(i -> i.toString())
        .collect(Collectors.joining(", "))
    );
  }

  private static void testRankModels(ModelDBService.Client client) throws Exception {
    // Store a few models.
    int m1Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int m2Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;
    int m3Id = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    LinearModel lm1 = StructFactory.makeLinearModel();
    LinearModel lm2 = StructFactory.makeLinearModel();
    LinearModel lm3 = StructFactory.makeLinearModel();
    client.storeLinearModel(m1Id, lm1);
    client.storeLinearModel(m2Id, lm2);
    client.storeLinearModel(m3Id, lm3);

    // Rank the models.
    System.out.println("Ranking models by RMSE");
    System.out.println(m1Id + "=" + lm1.rmse);
    System.out.println(m2Id + "=" + lm2.rmse);
    System.out.println(m3Id + "=" + lm3.rmse);
    System.out.println(client.rankModels(Arrays.asList(m1Id, m2Id, m3Id), ModelRankMetric.RMSE));
  }

  public static void testConfidenceIntervals(ModelDBService.Client client) throws Exception {
    // Store a model.
    int modelId = client.storeFitEvent(StructFactory.makeFitEvent()).modelId;

    // Store the linear model.
    client.storeLinearModel(modelId, StructFactory.makeLinearModel());

    // Compute the confidence intervals.
    System.out.println(client.confidenceIntervals(modelId, 0.05));
  }

  public static void testModelsWithFeatures(ModelDBService.Client client) throws Exception {
    // Store a model.
    FitEvent fe = StructFactory.makeFitEvent();
    int modelId = client.storeFitEvent(fe).modelId;

    // Compute the modelIds containing the given features.
    List<String> featureNames = new ArrayList<>(fe.featureColumns);
    featureNames.remove(0);
    System.out.println("Got model " + client.modelsWithFeatures(featureNames));
    System.out.println("Expected " + modelId);
  }

  public static void testDescendentModels(ModelDBService.Client client) throws Exception {
    // Create a tree like this:
    //            DF1
    //     DF2            DF3
    // DF4      DF5
     int df1 = client.storeTransformEvent(StructFactory.makeTransformEvent()).newDataFrameId;

     TransformEvent te = StructFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df1));
     int df2 = client.storeTransformEvent(te).newDataFrameId;

     te = StructFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df1));
     client.storeTransformEvent(te);

     te = StructFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df2));
     int df4 = client.storeTransformEvent(te).newDataFrameId;

     te = StructFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df2));
     int df5 = client.storeTransformEvent(te).newDataFrameId;

     // Now train models on DF4 and DF5.
     FitEvent fe = StructFactory.makeFitEvent();
     fe.setDf(fe.df.setId(df4));
     int mid1 = client.storeFitEvent(fe).modelId;

     fe = StructFactory.makeFitEvent();
     fe.setDf(fe.df.setId(df5));
     int mid2 = client.storeFitEvent(fe).modelId;

     // Now find the descendent models of DF1.
     System.out.println(client
       .modelsDerivedFromDataFrame(df1)
       .stream()
       .map(s -> s.toString())
       .collect(Collectors.joining(", ", "Descendents ", ""))
     );
     System.out.println("Expected descendents " + mid1 + " and " + mid2);
  }

  public static void testReadInfo(ModelDBService.Client client) throws Exception {
    // First create the model.
    FitEvent fe = StructFactory.makeFitEvent();
    FitEventResponse fer = client.storeFitEvent(fe);

    Transformer model = fe.model.setId(fer.modelId);

    // Store some metrics for the model.
    MetricEvent me1 = StructFactory.makeMetricEvent();
    MetricEvent me2 = StructFactory.makeMetricEvent();
    MetricEvent me3 = StructFactory.makeMetricEvent();
    me1.setModel(model);
    me2.setModel(model);
    me3.setModel(model);
    MetricEventResponse mer1 = client.storeMetricEvent(me1);
    me2.setDf(me1.df.setId(mer1.dfId));
    me2.setMetricType("recall");
    client.storeMetricEvent(me2);
    client.storeMetricEvent(me3);

    // Store some annotations for the model.
    AnnotationEvent ae = new AnnotationEvent(
      Arrays.asList(
        new AnnotationFragment("transformer", null, null, fe.model, "none"),
        new AnnotationFragment("message", null, null, fe.model, "was stored")
      ),
      fe.experimentRunId
    );
    client.storeAnnotationEvent(ae);


    // Get all the info for the model.
    System.out.println(client.getModel(model.getId()));
  }

  private static void testReadRunsInExperiment(ModelDBService.Client client) throws Exception {
    System.out.println(client.getRunsInExperiment(1));
  }

  private static void testReadRunsAndExperimentsInProject(ModelDBService.Client client) throws Exception {
    System.out.println(client.getRunsAndExperimentsInProject(1));
  }

  private static void testProjectOverviews(ModelDBService.Client client) throws Exception {
    System.out.println(client.getProjectOverviews());
  }

  private static void testExperimentRunDetails(ModelDBService.Client client) throws Exception {
    ExperimentRunDetailsResponse resp = client.getExperimentRunDetails(1);
    // Printing the full ModelResponses will clutter the console, so let's just get the IDs of the models.
    String modelids = resp
      .modelResponses
      .stream()
      .map(ModelResponse::getId)
      .map(r -> r.toString())
      .collect(Collectors.joining(","));

    System.out.println(modelids);
    System.out.println(resp.project);
    System.out.println(resp.experiment);
    System.out.println(resp.experimentRun);
  }

  /**
   * Test the connection.
   */
  private static void testConnection(ModelDBService.Client client) throws Exception {
    System.out.println(client.testConnection());
  }


  public static void main(String[] args) throws Exception {
    try {
      ModelDbConfig config = ModelDbConfig.parse(args);
      TTransport transport;

      transport = new TSocket(config.thriftHost, config.thriftPort);
      transport.open();

      TFramedTransport framed = new TFramedTransport(transport);

      TProtocol protocol = new TBinaryProtocol(framed);
      ModelDBService.Client client = new ModelDBService.Client(protocol);

      // Try every call to the server.
      runAllTests(client);

      transport.close();
    } catch (TTransportException e) {
      e.printStackTrace();
    }
  }
}
