package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.JobServiceGrpc.JobServiceBlockingStub;
import ai.verta.modeldb.JobStatusEnum.JobStatus;
import ai.verta.modeldb.JobTypeEnum.JobType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobTest {

  private static final Logger LOGGER = LogManager.getLogger(JobTest.class);
  private static Job testJob = null;
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    App app = App.getInstance();
    AuthService authService = new PublicAuthServiceUtils();
    RoleService roleService = new PublicRoleServiceUtils(authService);

    Map<String, Object> authServicePropMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.AUTH_SERVICE);
    if (authServicePropMap != null) {
      String authServiceHost = (String) authServicePropMap.get(ModelDBConstants.HOST);
      Integer authServicePort = (Integer) authServicePropMap.get(ModelDBConstants.PORT);
      app.setAuthServerHost(authServiceHost);
      app.setAuthServerPort(authServicePort);

      authService = new AuthServiceUtils();
      roleService = new RoleServiceUtils(authService);
    }

    App.initializeServicesBaseOnDataBase(
        serverBuilder, databasePropMap, propertiesMap, authService, roleService);
    serverBuilder.intercept(new ModelDBAuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);
  }

  @After
  public void clientClose() {
    if (!channel.isShutdown()) {
      channel.shutdownNow();
    }
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
  }

  @Test
  public void a_jobCreateTest() {
    LOGGER.info("Create Job test start................................");

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    List<KeyValue> metadataList = new ArrayList<>();
    for (int count = 0; count < 3; count++) {
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "Job metadata_"
                      + count
                      + "_"
                      + Calendar.getInstance().getTimeInMillis()
                      + "_value")
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey("Job metadata_" + count + "_" + Calendar.getInstance().getTimeInMillis())
              .setValue(stringValue)
              .build();
      metadataList.add(keyValue);
    }

    CreateJob request =
        CreateJob.newBuilder()
            .setDescription("This is a job description.")
            .setStartTime(String.valueOf(Calendar.getInstance().getTimeInMillis()))
            .addAllMetadata(metadataList)
            .setJobStatus(JobStatus.NOT_STARTED)
            .setJobType(JobType.KUBERNETES_JOB)
            .build();

    CreateJob.Response value = jobServiceStub.createJob(request);
    testJob = value.getJob();
    try {
      LOGGER.info("Job detail : \n" + JsonFormat.printer().print(testJob));
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    LOGGER.info("Job Created Successfully..");
    assertEquals(request.getStartTime(), testJob.getStartTime());
    LOGGER.info("Create Job test stop................................");
  }

  @Test
  public void aa_jobCreateNegativeTest() {
    LOGGER.info("Create Job Negative test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    CreateJob request =
        CreateJob.newBuilder()
            .setDescription("This is a job description.")
            .setJobStatus(JobStatus.NOT_STARTED)
            .setJobType(JobType.KUBERNETES_JOB)
            .build();

    try {
      jobServiceStub.createJob(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("Create Job Negative test stop................................");
  }

  @Test
  public void b_updateJobTest() throws IOException {
    LOGGER.info("Update Job test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    UpdateJob updateJob =
        UpdateJob.newBuilder().setId(testJob.getId()).setJobStatus(JobStatus.IN_PROGRESS).build();

    UpdateJob.Response jobUpdateResponse = jobServiceStub.updateJob(updateJob);
    testJob = jobUpdateResponse.getJob();
    LOGGER.info("Job detail : \n" + JsonFormat.printer().print(testJob));
    assertEquals(updateJob.getJobStatus(), testJob.getJobStatus());
    d_deleteJobTest();
    LOGGER.info("Update Job test stop................................");
  }

  @Test
  public void b_updateJobNegativeTest() {
    LOGGER.info("Update Job Negative test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    UpdateJob updateJob = UpdateJob.newBuilder().setJobStatus(JobStatus.IN_PROGRESS).build();

    try {
      jobServiceStub.updateJob(updateJob);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("Update Job test Negative stop................................");
  }

  @Test
  public void c_getJobTest() throws IOException {
    LOGGER.info("Get Job test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    GetJob getJobRequest = GetJob.newBuilder().setId(testJob.getId()).build();
    GetJob.Response getJobResponse = jobServiceStub.getJob(getJobRequest);
    testJob = getJobResponse.getJob();
    LOGGER.info("Job detail : \n" + JsonFormat.printer().print(testJob));
    assertEquals(getJobRequest.getId(), testJob.getId());
    d_deleteJobTest();
    LOGGER.info("Get Job test stop................................");
  }

  @Test
  public void c_getJobNegativeTest() {
    LOGGER.info("Get Job Negative test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    GetJob getJobRequest = GetJob.newBuilder().build();

    try {
      jobServiceStub.getJob(getJobRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("Get Job Negative test stop................................");
  }

  @Test
  public void d_deleteJobTest() {
    LOGGER.info("Delete Job test start................................");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    DeleteJob deleteJobRequest = DeleteJob.newBuilder().setId(testJob.getId()).build();

    DeleteJob.Response getJobResponse = jobServiceStub.deleteJob(deleteJobRequest);
    if (getJobResponse.getStatus()) {
      testJob = null;
    }
    assertTrue(getJobResponse.getStatus());
    LOGGER.info("Delete Job test stop................................");
  }
}
