package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.ModelDBException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;

public interface LineageDAO {

  /**
   * Create and log a Lineage.
   *
   * @param addLineage : request information
   * @return {@link AddLineage.Response} : status
   * @throws ModelDBException wrong data format
   */
  AddLineage.Response addLineage(
      AddLineage addLineage,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  /**
   * Delete a Lineage.
   *
   * @param deleteLineage : request information
   * @return {@link DeleteLineage.Response} : status
   * @throws ModelDBException wrong data format
   */
  DeleteLineage.Response deleteLineage(
      DeleteLineage deleteLineage, CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException, InvalidProtocolBufferException;

  /**
   * Find all inputs of specified Lineages.
   *
   * @param findAllInputs : request information
   * @return {@link FindAllInputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllInputs.Response findAllInputs(
      FindAllInputs findAllInputs,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException, InvalidProtocolBufferException;

  /**
   * Find all outputs of specified Lineages.
   *
   * @param findAllOutputs : request information
   * @return {@link FindAllOutputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllOutputs.Response findAllOutputs(
      FindAllOutputs findAllOutputs,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException, InvalidProtocolBufferException;

  /**
   * Find all inputs and outputs of specified Lineages.
   *
   * @param findAllInputsOutputs : request information
   * @return {@link FindAllInputsOutputs.Response} : status
   * @throws ModelDBException wrong data format
   */
  FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException, InvalidProtocolBufferException;
}
