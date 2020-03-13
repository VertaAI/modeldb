package ai.verta.modeldb.job;

import ai.verta.modeldb.Job;
import ai.verta.modeldb.JobStatusEnum.JobStatus;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;

public interface JobDAO {

  /**
   * Validate if the user is owner of the entity or not. If UserInfo is null then system assumes
   * that Authentication is disabled and returns true for further process.
   *
   * @param entityFieldKey --> key like ModelDBConstants.ID, ModelDBConstants.NAME etc.
   * @param entityFieldValue --> value of key like job.id etc.
   * @param userInfo --> UserInfo provided by AuthService
   */
  void validateEntityUser(String entityFieldKey, String entityFieldValue, UserInfo userInfo);

  /**
   * Insert Job entity in database.
   *
   * @param Job job
   * @return Job insertedJob
   * @throws InvalidProtocolBufferException
   */
  Job insertJob(Job job) throws InvalidProtocolBufferException;

  /**
   * Get Job entity using given jobId from database.
   *
   * @param entityFieldKey --> key like ModelDBConstants.ID, ModelDBConstants.NAME etc.
   * @param entityFieldValue --> value of key like job.id etc.
   * @return Job job
   * @throws InvalidProtocolBufferException
   */
  Job getJob(String entityFieldKey, String entityFieldValue) throws InvalidProtocolBufferException;

  /**
   * Delete the Job from database using jobId.
   *
   * @param String jobId
   * @return Boolean updated status
   */
  Boolean deleteJob(String jobId);

  /**
   * Update Job entity(jobStatus, endTime) in database using jobId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String jobId
   * @param String jobStatus
   * @param String endTime
   * @return Job updatedJob
   * @throws InvalidProtocolBufferException
   */
  Job updateJob(String jobId, JobStatus jobStatus, String endTime)
      throws InvalidProtocolBufferException;
}
