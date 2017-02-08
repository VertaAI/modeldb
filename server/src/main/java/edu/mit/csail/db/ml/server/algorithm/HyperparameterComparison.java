package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import modeldb.CompareHyperParametersResponse;
import modeldb.ResourceNotFoundException;
import modeldb.StringPair;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HyperparameterComparison {
  /**
   * Compare the hyperparameters between two models.
   * @param modelId1 - The ID of the first model.
   * @param modelId2 - The ID of the second model.
   * @param ctx - The database context.
   * @return The comparison of hyperparameters.
   */
  public static CompareHyperParametersResponse compareHyperParameters(int modelId1, int modelId2, DSLContext ctx)
    throws ResourceNotFoundException {
    String ERROR_FORMAT = "Could not find TransformerSpec for Transformer %d because it doesn't exist";
    // Fetch the specs associated with the models.
    Record1<Integer> rec1 = ctx
      .select(Tables.FITEVENT.TRANSFORMERSPEC)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId1))
      .fetchOne();
    if (rec1 == null) {
      throw new ResourceNotFoundException(String.format(ERROR_FORMAT, modelId1));
    }
    int spec1 = rec1.value1();

    Record1<Integer> rec2 = ctx
      .select(Tables.FITEVENT.TRANSFORMERSPEC)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId2))
      .fetchOne();
    if (rec2 == null) {
      throw new ResourceNotFoundException(String.format(ERROR_FORMAT, modelId2));
    }
    int spec2  = rec2.value1();

    // Fetch the hyperparameters for the specs.
    Map<String, String> params1 = ctx
      .select(Tables.HYPERPARAMETER.PARAMNAME, Tables.HYPERPARAMETER.PARAMVALUE)
      .from(Tables.HYPERPARAMETER)
      .where(Tables.HYPERPARAMETER.SPEC.eq(spec1))
      .fetch()
      .map(rec -> new Pair<>(rec.value1(), rec.value2()))
      .stream()
      .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    Map<String, String> params2 = ctx
      .select(Tables.HYPERPARAMETER.PARAMNAME, Tables.HYPERPARAMETER.PARAMVALUE)
      .from(Tables.HYPERPARAMETER)
      .where(Tables.HYPERPARAMETER.SPEC.eq(spec2))
      .fetch()
      .map(rec -> new Pair<>(rec.value1(), rec.value2()))
      .stream()
      .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));;

    // Find the hyperparameters that are common to both specs.
    Set<String> commonHyperParameters = new HashSet<String>(params1.keySet()) ;
    commonHyperParameters.retainAll(params2.keySet());

    Map<String, StringPair> commonHyperparameterMap = commonHyperParameters
      .stream()
      .map(name -> new Pair<>(name, new StringPair(params1.get(name), params2.get(name))))
      .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    // Get the hyperparameter maps for the models.
    Map<String, String> model1Hyperparameters = params1
      .entrySet()
      .stream()
      .filter(pair -> !commonHyperParameters.contains(pair.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<String, String> model2Hyperparameters = params2
      .entrySet()
      .stream()
      .filter(pair -> !commonHyperParameters.contains(pair.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new CompareHyperParametersResponse(
      model1Hyperparameters,
      model2Hyperparameters,
      commonHyperparameterMap
    );
  }
}
