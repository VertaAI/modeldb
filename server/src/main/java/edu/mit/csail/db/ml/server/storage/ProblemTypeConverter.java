package edu.mit.csail.db.ml.server.storage;

import modeldb.ProblemType;

public class ProblemTypeConverter {
  private static final String BINARY_CLASSIFICATION_STRING = "binary classification";
  private static final String MULTICLASS_CLASSIFICATION_STRING = "multiclass classification";
  private static final String REGRESSION_STRING = "regression";
  private static final String CLUSTERING_STRING = "clustering";
  private static final String RECOMMENDATION_STRING = "recommendation";
  private static final String UNDEFINED_STRING = "undefined";


  public static String toString(ProblemType problemType) {
    switch (problemType) {
      case BINARY_CLASSIFICATION: return BINARY_CLASSIFICATION_STRING;
      case MULTICLASS_CLASSIFICATION: return MULTICLASS_CLASSIFICATION_STRING;
      case REGRESSION: return REGRESSION_STRING;
      case CLUSTERING: return CLUSTERING_STRING;
      case RECOMMENDATION: return RECOMMENDATION_STRING;
      default: return UNDEFINED_STRING;
    }
  }

  public static ProblemType fromString(String problemType) {
    switch (problemType) {
      case BINARY_CLASSIFICATION_STRING: return ProblemType.BINARY_CLASSIFICATION;
      case MULTICLASS_CLASSIFICATION_STRING: return ProblemType.MULTICLASS_CLASSIFICATION;
      case REGRESSION_STRING: return ProblemType.REGRESSION;
      case CLUSTERING_STRING: return ProblemType.CLUSTERING;
      case RECOMMENDATION_STRING: return ProblemType.RECOMMENDATION;
      default: return ProblemType.UNDEFINED;
    }
  }


}
