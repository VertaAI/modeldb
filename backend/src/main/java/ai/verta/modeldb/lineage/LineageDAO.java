package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputs.Response;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;
import org.hibernate.Session;

public interface LineageDAO {

  /**
   * Create and log a Lineage.
   *
   * @param addLineage : request information
   * @return {@link AddLineage.Response} : status
   * @throws ModelDBException wrong data format
   */
  AddLineage.Response addLineage(
      AddLineage addLineage, ResourceExistsCheckConsumer resourceExistsCheckConsumer)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  /**
   * Delete a Lineage.
   *
   * @param deleteLineage : request information
   * @return {@link DeleteLineage.Response} : status
   * @throws ModelDBException wrong data format
   */
  DeleteLineage.Response deleteLineage(
      DeleteLineage deleteLineage, ResourceExistsCheckConsumer resourceExistsCheckConsumer)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  /**
   * Find all inputs of specified Lineages.
   *
   * @param findAllInputs : request information
   * @param t
   * @return {@link FindAllInputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllInputs.Response findAllInputs(
      FindAllInputs findAllInputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, Response, Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  /**
   * Find all outputs of specified Lineages.
   *
   * @param findAllOutputs : request information
   * @param t
   * @return {@link FindAllOutputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllOutputs.Response findAllOutputs(
      FindAllOutputs findAllOutputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, FindAllOutputs.Response, FindAllOutputs.Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  /**
   * Find all inputs and outputs of specified Lineages.
   *
   * @param findAllInputsOutputs : request information
   * @param t
   * @return {@link FindAllInputsOutputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, FindAllInputsOutputs.Response, FindAllInputsOutputs.Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;
}
