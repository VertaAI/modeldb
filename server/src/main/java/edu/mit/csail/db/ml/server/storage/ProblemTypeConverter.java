package edu.mit.csail.db.ml.server.storage;

import modeldb.ProblemType;

/**
 * This class contains logic for converting between the string and enum representations of the ProblemType.
 * We need this because ProblemType is a thrift structure, and it thus represented by an enum. However, in order to
 * store it in the database, we need to store it as a string.
 */
public class ProblemTypeConverter {
  /**
   * Represents a problem where we give each example one of two possible labels.
   */
  private static final String BINARY_CLASSIFICATION_STRING = "binary classification";

  /**
   * Represents a problem where we give each example one of three (or more) possible labels.
   */
  private static final String MULTICLASS_CLASSIFICATION_STRING = "multiclass classification";

  /**
   * Represents a problem where we predict a real valued output (or a vector of real valued outputs) for each example.
   */
  private static final String REGRESSION_STRING = "regression";

  /**
   * Represents a problem where we group together examples in an unsupervised way.
   */
  private static final String CLUSTERING_STRING = "clustering";

  /**
   * Represents a problem where we select recommendations for a given user.
   */
  private static final String RECOMMENDATION_STRING = "recommendation";

  /**
   * Represents a problem that does not fit into any of the above categories.
   */
  private static final String UNDEFINED_STRING = "undefined";


  /**
   * @param problemType - The problem type.
   * @return A string representation of the problem type.
   */
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

  /**
   * @param problemType - The string representation of the problem type (see the constants above).
   * @return The problem type object.
   */
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
