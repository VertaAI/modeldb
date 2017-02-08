package edu.mit.csail.db.ml;

import modeldb.*;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class exposes static methods for creating dummy Thrift structures
 * that can be used by the testing code.
 */
public class StructFactory {
  private static final Random generator = new Random();

  /**
   * This constant simply indicates that a primary key does not exist and should be generated.
   */
  private static final int EMPTY_PRIMARY_KEY = -1;

  public static Project makeProject() {
    return new Project(
      EMPTY_PRIMARY_KEY,
      "name",
      "author",
      "description"
    );
  }

  public static Experiment makeExperiment() {
    return new Experiment(
      EMPTY_PRIMARY_KEY,
      1, // The experiment is in the project with ID = 1.
      "test experiment",
      "this is a test experiment",
      false // This experiment is NOT the default experiment.
    );
  }

  public static ExperimentRun makeExperimentRun() {
    return new ExperimentRun(
      EMPTY_PRIMARY_KEY,
      1,
      "experiment run description"
    );
  }

  private static String makeTag() {
    return "tag " + generator.nextInt();
  }

  private static DataFrameColumn makeDataFrameColumn() {
    return new DataFrameColumn(
      "col" + generator.nextInt(),
      "double"
    );
  }

  public static DataFrame makeDataFrame() {
    return new DataFrame(
      EMPTY_PRIMARY_KEY,
      IntStream.range(0, 5).mapToObj(s -> makeDataFrameColumn()).collect(Collectors.toList()),
      Math.abs(generator.nextInt()),
      makeTag()
    );
  }

  public static Transformer makeTransformer() {
    return new Transformer(
      EMPTY_PRIMARY_KEY,
      "linear regression",
      makeTag()
    );
  }

  public static TransformEvent makeTransformEvent() {
    return new TransformEvent(
      makeDataFrame(),
      makeDataFrame(),
      makeTransformer(),
      Arrays.asList("inCol1", "inCol2"),
      Arrays.asList("outCol1", "outCol2", "outCol3"),
      1
    );
  }

  private static HyperParameter makeHyperparameter() {
    return new HyperParameter(
      "hyperparam" + generator.nextInt(),
      "someval" + generator.nextInt(),
      "string",
      0,
      0
    );
  }

  private static HyperParameter makeSharedHyperparameter() {
    return new HyperParameter(
      "hyperparam",
      "someval" + generator.nextInt(),
      "string",
      0,
      0
    );
  }

  private static HyperParameter makeStandardizationHyperparameter() {
    return new HyperParameter(
      "standardization",
      "true",
      "string",
      0,
      0
    );
  }

  public static TransformerSpec makeTransformerSpec() {
    return new TransformerSpec(
      EMPTY_PRIMARY_KEY,
      "lin reg",
      Arrays.asList(
        makeHyperparameter(),
        makeHyperparameter(),
        makeSharedHyperparameter(),
        makeStandardizationHyperparameter()
      ),
      makeTag()
    );
  }

  public static AnnotationFragment makeAnnotationFragment() {
    return new AnnotationFragment(
      new String[] {"message", "dataframe", "transformer", "spec"}[generator.nextInt(4)],
      makeDataFrame(),
      makeTransformerSpec(),
      makeTransformer(),
      "message" + generator.nextInt()
    );
  }

  public static AnnotationEvent makeAnnotationEvent() {
    return new AnnotationEvent(
      IntStream.range(0, 3).mapToObj(s -> makeAnnotationFragment()).collect(Collectors.toList()),
      1
    );
  }

  public static FitEvent makeFitEvent() {
    ProblemType[] ptypes = new ProblemType[] {
      ProblemType.BINARY_CLASSIFICATION,
      ProblemType.MULTICLASS_CLASSIFICATION,
      ProblemType.REGRESSION,
      ProblemType.RECOMMENDATION,
      ProblemType.CLUSTERING
    };
    return new FitEvent(
      makeDataFrame(),
      makeTransformerSpec(),
      makeTransformer(),
      Arrays.asList("featCol1", "featCol2", "feat" + generator.nextInt()),
      Arrays.asList("predCol1", "predCol2"),
      Arrays.asList("labCol1", "labCol2"),
      1
    ).setProblemType(ptypes[generator.nextInt(ptypes.length)]);
  }

  public static RandomSplitEvent makeRandomSplitEvent() {
    return new RandomSplitEvent(
      makeDataFrame(),
      Arrays.asList(0.4, 0.5),
      0,
      Arrays.asList(makeDataFrame(), makeDataFrame()),
      1
    );
  }

  public static MetricEvent makeMetricEvent() {
    return new MetricEvent(
      makeDataFrame(),
      makeTransformer(),
      "precision",
      0.9,
      "labelCol",
      "predictionCol",
      1
    );
  }

  public static ProjectEvent makeProjectEvent() {
    return new ProjectEvent(makeProject());
  }

  public static ExperimentEvent makeExperimentEvent() {
    return new ExperimentEvent(makeExperiment());
  }

  public static ExperimentRunEvent makeExperimentRunEvent() {
    return new ExperimentRunEvent(makeExperimentRun());
  }

  public static PipelineEvent makePipelineEvent() {
    return new PipelineEvent(
      makeFitEvent(),
      IntStream
        .range(1, 4)
        .mapToObj(ind -> new PipelineTransformStage(ind, makeTransformEvent()))
        .collect(Collectors.toList()),
      IntStream
      .range(4, 8)
      .mapToObj(ind -> new PipelineFitStage(ind, makeFitEvent()))
      .collect(Collectors.toList()),
      1
    );
  }

  public static CrossValidationFold makeCrossValidationFold() {
    return new CrossValidationFold(
      makeTransformer(),
      makeDataFrame(),
      makeDataFrame(),
      generator.nextDouble()
    );
  }

  public static CrossValidationEvent makeCrossValidationEvent() {
    return new CrossValidationEvent(
      makeDataFrame(),
      makeTransformerSpec(),
      generator.nextLong(),
      "evaluator",
      Arrays.asList("label col1", "label col 2"),
      Arrays.asList("prediction col1", "prediction col 2"),
      Arrays.asList("feature col1", "feature col 2"),
      Arrays.asList(makeCrossValidationFold(), makeCrossValidationFold()),
      1
    );
  }

  public static LinearModelTerm makeLinearModelTerm() {
    LinearModelTerm lmt = new LinearModelTerm(generator.nextDouble());
    lmt.setStdErr(generator.nextDouble());
    lmt.setPValue(generator.nextDouble());
    lmt.setTStat(generator.nextDouble());
    return lmt;
  }

  public static LinearModel makeLinearModel() {
    LinearModel lm = new LinearModel(
      IntStream
        .range(0, 3)
        .mapToObj(i -> makeLinearModelTerm())
        .collect(Collectors.toList())
    );
    lm.setInterceptTerm(makeLinearModelTerm());
    lm.setObjectiveHistory(
      IntStream
        .range(0, generator.nextInt(10))
        .mapToDouble(s -> generator.nextDouble())
        .boxed()
        .collect(Collectors.toList())
    );
    lm.setR2(generator.nextDouble());
    lm.setRmse(generator.nextDouble());
    lm.setExplainedVariance(generator.nextDouble());
    return lm;
  }

  public static GridSearchCrossValidationEvent makeGridSearchCrossValidationEvent() {
    return new GridSearchCrossValidationEvent(
      3,
      makeFitEvent(),
      Arrays.asList(makeCrossValidationEvent(), makeCrossValidationEvent()),
      1
    );
  }
}
