package ai.verta.modeldb.job;

import ai.verta.modeldb.CreateJob;
import ai.verta.modeldb.DeleteJob;
import ai.verta.modeldb.GetJob;
import ai.verta.modeldb.Job;
import ai.verta.modeldb.JobServiceGrpc.JobServiceImplBase;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.UpdateJob;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JobServiceImpl extends JobServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(JobServiceImpl.class);
  private AuthService authService;
  private JobDAO jobDAO;

  public JobServiceImpl(AuthService authService, JobDAO jobDAO) {
    this.authService = authService;
    this.jobDAO = jobDAO;
  }

  /**
   * The method gets UserInfo from context. If UserInfo is Null, then system assumes that
   * AuthService is disabled and returns. If user has some value then it validates the user by
   * calling the DAO method for user validation wherein if user is not owner of the entity then it
   * throws exception else it continues for further process.
   *
   * @param entityFieldKey --> key like ModelDBConstants.ID, ModelDBConstants.NAME etc.
   * @param entityFieldValue --> value of key like job.id, job.status etc.
   */
  private void validateEntityUser(String entityFieldKey, String entityFieldValue) {
    // Get the user info from the Context
    UserInfo userInfo = authService.getCurrentLoginUserInfo();

    // If UserInfo is Null, then continue as auth service is disabled
    // Else If UserInfo has some value, then Validate the user
    if (userInfo != null) {
      jobDAO.validateEntityUser(entityFieldKey, entityFieldValue, userInfo);
    }
  }

  /**
   * Method to convert createJob request to Job object. This method generates the job Id using UUID
   * and puts it in Job object.
   *
   * @param CreateJob request
   * @param UserInfo userInfo
   * @return Job
   */
  private Job getJobFromRequest(CreateJob request, UserInfo userInfo) {

    if (request.getStartTime().isEmpty()) {
      String errorMessage = "Job start time not found in CreateJob request";
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateJob.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    /*
     * Create Job entity from given CreateJob request. generate UUID and put as id in
     * job for uniqueness. set above created List<KeyValue> attributes in job entity.
     */
    Job.Builder jobBuilder =
        Job.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setDescription(request.getDescription())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .addAllMetadata(request.getMetadataList())
            .setJobStatus(request.getJobStatus())
            .setJobType(request.getJobType());

    if (userInfo != null) {
      jobBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    return jobBuilder.build();
  }

  @Override
  public void createJob(CreateJob request, StreamObserver<CreateJob.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      Job job = getJobFromRequest(request, userInfo);
      job = jobDAO.insertJob(job);
      responseObserver.onNext(CreateJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(CreateJob.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getJob(GetJob request, StreamObserver<GetJob.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID not found in GetJob request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      validateEntityUser(ModelDBConstants.ID, request.getId());

      Job job = jobDAO.getJob(ModelDBConstants.ID, request.getId());
      responseObserver.onNext(GetJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetJob.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateJob(UpdateJob request, StreamObserver<UpdateJob.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID not found in UpdateJob request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      validateEntityUser(ModelDBConstants.ID, request.getId());

      Job job = jobDAO.updateJob(request.getId(), request.getJobStatus(), request.getEndTime());
      responseObserver.onNext(UpdateJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(UpdateJob.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteJob(DeleteJob request, StreamObserver<DeleteJob.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID not found in DeleteJob request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      validateEntityUser(ModelDBConstants.ID, request.getId());

      Boolean deletedStatus = jobDAO.deleteJob(request.getId());
      responseObserver.onNext(DeleteJob.Response.newBuilder().setStatus(deletedStatus).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteJob.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }
}
