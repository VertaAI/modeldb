package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.config.MDBConfig;
import java.util.regex.Pattern;

public class PredicateHandlerUtils {
  private static final MDBConfig mdbConfig = App.getInstance().mdbConfig;

  protected String columnAsNumber(String colName) {
    if (mdbConfig.getDatabase().getRdbConfiguration().isH2()) {
      return String.format("cast(trim('\"' from %s) as double precision)", colName);
    }

    return String.format("cast(%s as decimal(16, 8))", colName);
  }

  protected String applyOperator(
      OperatorEnum.Operator operator, String colName, String valueBinding) {
    /* NOTE: Here we have used reverse conversion of `NE` and `NOT_CONTAIN` to `EQ` and `CONTAIN` respectively
    We will manage `NE` and `NOT_CONTAIN` operator at bottom of the calling method of this function
    using `IN` OR `NOT IN` query
    */
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.GT_VALUE:
        return String.format("%s > %s", colName, valueBinding);
      case OperatorEnum.Operator.GTE_VALUE:
        return String.format("%s >= %s", colName, valueBinding);
      case OperatorEnum.Operator.LT_VALUE:
        return String.format("%s < %s", colName, valueBinding);
      case OperatorEnum.Operator.LTE_VALUE:
        return String.format("%s <= %s", colName, valueBinding);
      case OperatorEnum.Operator.NE_VALUE:
        return String.format("%s = %s", colName, valueBinding);
      case OperatorEnum.Operator.CONTAIN_VALUE:
      case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
        return String.format(
            "%s LIKE %s",
            "lower(" + colName + ") ", Pattern.compile(valueBinding).toString().toLowerCase());
      case OperatorEnum.Operator.IN_VALUE:
        return String.format("%s IN (%s)", colName, valueBinding);
      default:
        return String.format("%s = %s", colName, valueBinding);
    }
  }

  protected <T> Object wrapValue(OperatorEnum.Operator operator, T value) {
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.CONTAIN_VALUE:
      case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
        return String.format("%%%s%%", String.valueOf(value).toLowerCase());
      default:
        return value;
    }
  }
}
