package ai.verta.modeldb.job;

import ai.verta.modeldb.App;
import ai.verta.modeldb.Job;
import ai.verta.modeldb.JobStatusEnum.JobStatus;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.entities.JobEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class JobDAORdbImpl implements JobDAO {

  private static final Logger LOGGER = LogManager.getLogger(JobDAORdbImpl.class);
  private final AuthService authService;
  private static final String GET_JOB_PREFIX_HQL = "FROM JobEntity jb ";
  private static final StringBuilder COUNT_QUERY_PREFIX =
      new StringBuilder().append("Select count(*) From ");

  public JobDAORdbImpl(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public void validateEntityUser(
      String entityFieldKey, String entityFieldValue, UserInfo userInfo) {
    App app = App.getInstance();
    if (app.getDisabledAuthz()) {
      return;
    }

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      StringBuilder queryStringBuilder =
          new StringBuilder()
              .append(COUNT_QUERY_PREFIX)
              .append(JobEntity.class.getSimpleName())
              .append(" entity where entity.")
              .append(entityFieldKey)
              .append(" = :value1 AND ")
              .append(ModelDBConstants.OWNER)
              .append(" =: value2");
      Query query = session.createQuery(queryStringBuilder.toString());
      query.setParameter("value1", entityFieldValue);
      query.setParameter("value2", authService.getVertaIdFromUserInfo(userInfo));
      Long count = (Long) query.uniqueResult();
      if (!(count > 0)) {
        Status statusMessage =
            Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage("Access is denied. User is unauthorized for given entity")
                .build();
        throw StatusProto.toStatusRuntimeException(statusMessage);
      }
    }
  }

  @Override
  public Job insertJob(Job job) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      JobEntity jobObj = RdbmsUtils.generateJobEntity(job);
      session.saveOrUpdate(jobObj);
      transaction.commit();
      LOGGER.debug("Job inserted successfully");
      return jobObj.getProtoObject();
    }
  }

  @Override
  public Job getJob(String entityFieldKey, String entityFieldValue)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      String finalQuery =
          GET_JOB_PREFIX_HQL + " WHERE jb." + entityFieldKey + " = :entityFieldValue ";
      Query query = session.createQuery(finalQuery);
      query.setParameter("entityFieldValue", entityFieldValue);
      JobEntity jobObj = (JobEntity) query.uniqueResult();
      if (jobObj == null) {
        LOGGER.warn(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Job getting successfully");
      return jobObj.getProtoObject();
    }
  }

  @Override
  public Boolean deleteJob(String jobId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      JobEntity jobEntity = session.load(JobEntity.class, jobId);
      if (jobEntity == null) {
        LOGGER.warn(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      session.delete(jobEntity);
      transaction.commit();
      LOGGER.debug("Job deleted successfully");
      return true;
    }
  }

  @Override
  public Job updateJob(String jobId, JobStatus jobStatus, String endTime)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      JobEntity jobEntity = session.get(JobEntity.class, jobId);
      if (jobEntity == null) {
        LOGGER.warn(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.JOB_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      jobEntity.setJob_status(jobStatus.ordinal());
      jobEntity.setEnd_time(endTime);
      session.saveOrUpdate(jobEntity);

      transaction.commit();
      LOGGER.debug("Job updated successfully");
      return jobEntity.getProtoObject();
    }
  }
}
