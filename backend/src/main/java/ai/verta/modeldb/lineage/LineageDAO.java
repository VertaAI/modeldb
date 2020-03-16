package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.ModelDBException;

public interface LineageDAO {

  /**
   * Create and log a Lineage.
   *
   * @param addLineage : request information
   * @return {@link AddLineage.Response} : status
   * @throws ModelDBExceptionWithCode wrong data format
   */
  Response addLineage(AddLineage addLineage) throws ModelDBException;

  /**
   * Delete a Lineage.
   *
   * @param deleteLineage : request information
   * @return {@link DeleteLineage.Response} : status
   * @throws ModelDBExceptionWithCode wrong data format
   */
  DeleteLineage.Response deleteLineage(DeleteLineage deleteLineage) throws ModelDBException;

  /**
   * Find all inputs of specified Lineages.
   *
   * @param findAllInputs : request information
   * @return {@link FindAllInputs.Response} : status
   * @throws ModelDBExceptionWithCode wrong data format
   */
  FindAllInputs.Response findAllInputs(FindAllInputs findAllInputs) throws ModelDBException;

  /**
   * Find all outputs of specified Lineages.
   *
   * @param findAllOutputs : request information
   * @return {@link FindAllOutputs.Response} : status
   * @throws ModelDBExceptionWithCode wrong data format
   */
  FindAllOutputs.Response findAllOutputs(FindAllOutputs findAllOutputs) throws ModelDBException;

  /**
   * Find all inputs and outputs of specified Lineages.
   *
   * @param findAllInputsOutputs : request information
   * @return {@link FindAllInputsOutputs.Response} : status
   * @throws ModelDBExceptionWithCode wrong data format
   */
  FindAllInputsOutputs.Response findAllInputsOutputs(FindAllInputsOutputs findAllInputsOutputs)
      throws ModelDBException;
}
