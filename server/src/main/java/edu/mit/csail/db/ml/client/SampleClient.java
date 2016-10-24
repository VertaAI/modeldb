package edu.mit.csail.db.ml.client;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import modeldb.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jooq.util.derby.sys.Sys;

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
    testLinearModelFeatureImportancesTwoModels(client);
    testIterationsUntilConvergence(client);
    testRankModels(client);
    testConfidenceIntervals(client);
    testModelsWithFeatures(client);
    testDescendentModels(client);
  }

  private static void testTransformEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeTransformEvent(DummyFactory.makeTransformEvent()));
  }

  private static void testAnnotationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeAnnotationEvent(DummyFactory.makeAnnotationEvent()));
  }

  private static void testFitEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeFitEvent(DummyFactory.makeFitEvent()));
  }

  private static void testRandomSplitEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeRandomSplitEvent(DummyFactory.makeRandomSplitEvent()));
  }

  private static void testMetricEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeMetricEvent(DummyFactory.makeMetricEvent()));
  }

  private static void testProjectEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeProjectEvent(DummyFactory.makeProjectEvent()));
  }

  private static void testExperimentEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeExperimentEvent(DummyFactory.makeExperimentEvent()));
  }

  private static void testExperimentRunEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeExperimentRunEvent(DummyFactory.makeExperimentRunEvent()));
  }

  private static void testPipelineEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storePipelineEvent(DummyFactory.makePipelineEvent()));
  }

  private static void testCrossValidationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeCrossValidationEvent(DummyFactory.makeCrossValidationEvent()));
  }

  private static void testGridSearchCrossValidationEvent(ModelDBService.Client client) throws Exception {
    System.out.println(client.storeGridSearchCrossValidationEvent(DummyFactory.makeGridSearchCrossValidationEvent()));
  }

  private static void testDataFrameAncestry(ModelDBService.Client client) throws Exception {
    for (int i = 0; i < 4; i++) {
      client.storeTransformEvent(DummyFactory.makeTransformEvent());
    }
    System.out.println(client.getDataFrameAncestry(3));
  }

  private static void testCommonAncestor(ModelDBService.Client client) throws Exception {
    // Store a TransformEvent to ensure that we have a DataFrame with ID = 1.
    client.storeTransformEvent(DummyFactory.makeTransformEvent());

    // Create another TransformEvent that creates a model that branches off DataFrame 1.
    TransformEvent te = DummyFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(1));
    TransformEventResponse resp = client.storeTransformEvent(te);
    int commonAncestorId = resp.newDataFrameId;

    // Create two TransformEvents that branch off the DataFrame created above.
    te = DummyFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(commonAncestorId));
    int dfId1 = client.storeTransformEvent(te).newDataFrameId;

    te = DummyFactory.makeTransformEvent();
    te.setOldDataFrame(te.getOldDataFrame().setId(commonAncestorId));
    int dfId2 = client.storeTransformEvent(te).newDataFrameId;

    // Now find the common ancestor of those two DataFrames, it should be the commonAncestorId.
    System.out.println(client.getCommonAncestor(dfId1, dfId2));

    // Now create FitEvents for dfId1 and dfId2.
    FitEvent fe = DummyFactory.makeFitEvent();
    fe.setDf(fe.getDf().setId(dfId1));
    int modelId1 = client.storeFitEvent(fe).modelId;

    fe = DummyFactory.makeFitEvent();
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
      modelIds.add(client.storeFitEvent(DummyFactory.makeFitEvent()).modelId);
    }

    // Now get the number of rows in each model.
    System.out.println(client.getTrainingRowsCount(modelIds.get(0)));
    System.out.println(client.getTrainingRowsCounts(modelIds));
  }

  private static void testHyperparameterComparison(ModelDBService.Client client) throws Exception {
    // Store two FitEvents.
    int model1Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int model2Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Compare the model's hyperparameters.
    System.out.println(client.compareHyperparameters(model1Id, model2Id));
  }

  private static void testFeatureComparison(ModelDBService.Client client) throws Exception {
    // Store two FitEvents.
    int model1Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int model2Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Compare the model's features.
    System.out.println(client.compareFeatures(model1Id, model2Id));
  }

  private static void testGroupByProblemType(ModelDBService.Client client) throws Exception {
    // Store some FitEvents.
    int NUM_EVENTS = 7;
    List<Integer> modelIds = new ArrayList<>();
    for (int i = 0; i < NUM_EVENTS; i++) {
      FitEvent fe = DummyFactory.makeFitEvent();
      modelIds.add(client.storeFitEvent(fe).modelId);
      System.out.println("Type = " + fe.problemType.toString() + " and id = " + modelIds.get(modelIds.size() - 1));
    }

    // Now group.
    System.out.println(client.groupByProblemType(modelIds));
  }

  private static void testSimilarModels(ModelDBService.Client client) throws Exception {
    // Store some FitEvents.
    for (int i = 0; i < 50; i++) {
      client.storeFitEvent(DummyFactory.makeFitEvent());
    }

    // Store another fit event and find the 3 models most similar to it.
    int modelId = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
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
    int modelId = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store a LinearModel.
    System.out.println(client.storeLinearModel(modelId, DummyFactory.makeLinearModel()));
  }

  private static void testLinearModelFeatureImportances(ModelDBService.Client client) throws Exception {
    // Store a FitEvent so that we have Transformer with ID = 1 in the database.
    int modelId = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store a LinearModel.
    client.storeLinearModel(modelId, DummyFactory.makeLinearModel());

    // Fetch the feature importances of the linear model.
    System.out.println(client.linearModelFeatureImportances(modelId));
  }

  private static void testLinearModelFeatureImportancesTwoModels(ModelDBService.Client client) throws Exception {
    // Store FitEvents so that we have Transformers with ID = 1 and ID = 2 in the database.
    int modelId1 = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int modelId2 = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    client.storeLinearModel(modelId1, DummyFactory.makeLinearModel());
    client.storeLinearModel(modelId2, DummyFactory.makeLinearModel());

    // Fetch feature importance comparison for them.
    System.out.println(client.compareLinearModelFeatureImportances(modelId1, modelId2));
  }

  private static void testIterationsUntilConvergence(ModelDBService.Client client) throws Exception {
    // Store a few models.
    int m1Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int m2Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int m3Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    client.storeLinearModel(m1Id, DummyFactory.makeLinearModel());
    client.storeLinearModel(m2Id, DummyFactory.makeLinearModel());
    client.storeLinearModel(m3Id, DummyFactory.makeLinearModel());

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
    int m1Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int m2Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;
    int m3Id = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store linear models for each.
    LinearModel lm1 = DummyFactory.makeLinearModel();
    LinearModel lm2 = DummyFactory.makeLinearModel();
    LinearModel lm3 = DummyFactory.makeLinearModel();
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
    int modelId = client.storeFitEvent(DummyFactory.makeFitEvent()).modelId;

    // Store the linear model.
    client.storeLinearModel(modelId, DummyFactory.makeLinearModel());

    // Compute the confidence intervals.
    System.out.println(client.confidenceIntervals(modelId, 0.05));
  }

  public static void testModelsWithFeatures(ModelDBService.Client client) throws Exception {
    // Store a model.
    FitEvent fe = DummyFactory.makeFitEvent();
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
     int df1 = client.storeTransformEvent(DummyFactory.makeTransformEvent()).newDataFrameId;

     TransformEvent te = DummyFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df1));
     int df2 = client.storeTransformEvent(te).newDataFrameId;

     te = DummyFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df1));
     client.storeTransformEvent(te);

     te = DummyFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df2));
     int df4 = client.storeTransformEvent(te).newDataFrameId;

     te = DummyFactory.makeTransformEvent();
     te.setOldDataFrame(te.oldDataFrame.setId(df2));
     int df5 = client.storeTransformEvent(te).newDataFrameId;

     // Now train models on DF4 and DF5.
     FitEvent fe = DummyFactory.makeFitEvent();
     fe.setDf(fe.df.setId(df4));
     int mid1 = client.storeFitEvent(fe).modelId;

     fe = DummyFactory.makeFitEvent();
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
