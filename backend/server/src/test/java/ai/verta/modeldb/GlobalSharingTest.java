package ai.verta.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.uac.AddUser;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.DeleteOrganization;
import ai.verta.uac.Organization;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetOrganization;
import ai.verta.uac.SetResource;
import io.grpc.StatusRuntimeException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalSharingTest extends TestsInit {

  private static final Logger LOGGER = LogManager.getLogger(GlobalSharingTest.class);

  private final ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT},
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET},
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY},
        });
  }

  public GlobalSharingTest(ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType) {
    this.resourceType = resourceType;
  }

  static class TestParameters {

    private final ResourceVisibility resourceVisibility;
    private final CollaboratorPermissions.Builder permissions;
    private final boolean readCheckResult;
    private final boolean writeCheckResult;

    public TestParameters(
        ResourceVisibility resourceVisibility,
        CollaboratorPermissions.Builder permissions,
        boolean readCheckResult,
        boolean writeCheckResult) {
      this.resourceVisibility = resourceVisibility;
      this.permissions = permissions;
      this.readCheckResult = readCheckResult;
      this.writeCheckResult = writeCheckResult;
    }

    public ResourceVisibility getResourceVisibility() {
      return resourceVisibility;
    }

    public CollaboratorPermissions.Builder getPermissions() {
      return permissions;
    }

    public boolean isReadCheckSuccess() {
      return readCheckResult;
    }

    public boolean isWriteCheckSuccess() {
      return writeCheckResult;
    }
  }

  private static final Collection<TestParameters> requestData =
      Arrays.asList(
          new TestParameters(
              ResourceVisibility.ORG_DEFAULT, CollaboratorPermissions.newBuilder(), true, false),
          new TestParameters(
              ResourceVisibility.ORG_CUSTOM,
              CollaboratorPermissions.newBuilder().setCollaboratorType(CollaboratorType.READ_WRITE),
              true,
              true),
          new TestParameters(
              ResourceVisibility.ORG_CUSTOM,
              CollaboratorPermissions.newBuilder().setCollaboratorType(CollaboratorType.READ_ONLY),
              true,
              false),
          new TestParameters(
              ResourceVisibility.PRIVATE, CollaboratorPermissions.newBuilder(), false, false));

  @Test
  public void createProjectWithGlobalSharingOrganization() {
    LOGGER.info("Global organization Project test start................................");

    if (!testConfig.hasAuth()) {
      Assert.assertTrue(true);
      return;
    }

    String orgName = "Org-test-verta-" + new Date().getTime();
    SetOrganization setOrganization =
        SetOrganization.newBuilder()
            .setOrganization(
                Organization.newBuilder()
                    .setName(orgName)
                    .setDescription("This is the verta test organization")
                    .build())
            .build();
    SetOrganization.Response orgResponse =
        organizationServiceBlockingStub.setOrganization(setOrganization);
    Organization organization = orgResponse.getOrganization();
    assertEquals(
        "Organization name not matched with expected organization name",
        orgName,
        organization.getName());

    organizationServiceBlockingStub.addUser(
        AddUser.newBuilder()
            .setOrgId(organization.getId())
            .setShareWith(authClientInterceptor.getClient2Email())
            .build());

    String orgResourceId = null;
    Repository repository = null;
    String orgResourceName;
    try {
      List<TestParameters> testParametersList = new LinkedList<>(requestData);
      TestParameters testParametersFirst =
          testParametersList.stream()
              .findFirst()
              .orElseThrow(() -> new RuntimeException("Request data is not specified"));
      testParametersList.remove(testParametersFirst);
      ResourceVisibility resourceVisibility = testParametersFirst.getResourceVisibility();
      CollaboratorPermissions.Builder customPermission = testParametersFirst.getPermissions();
      boolean isReadAllowed = testParametersFirst.isReadCheckSuccess();
      boolean isWriteAllowed = testParametersFirst.isWriteCheckSuccess();
      switch (resourceType) {
        case PROJECT:
          Map.Entry<String, String> result =
              createProject(organization, resourceVisibility, customPermission);
          orgResourceId = result.getKey();
          orgResourceName = result.getValue();
          break;
        case DATASET:
          result = createDataset(organization, resourceVisibility, customPermission);
          orgResourceId = result.getKey();
          orgResourceName = result.getValue();
          break;
        case REPOSITORY:
        default:
          repository = createRepository(orgName, resourceVisibility, customPermission);
          orgResourceId = String.valueOf(repository.getId());
          orgResourceName = repository.getName();
          break;
      }
      checkResourceActions(orgResourceId, repository, isReadAllowed, isWriteAllowed);
      for (TestParameters testParameters : requestData) {
        resourceVisibility = testParameters.getResourceVisibility();
        customPermission = testParameters.getPermissions();
        isReadAllowed = testParameters.isReadCheckSuccess();
        isWriteAllowed = testParameters.isWriteCheckSuccess();
        updateResource(
            organization, orgResourceId, orgResourceName, resourceVisibility, customPermission);
        checkResourceActions(orgResourceId, repository, isReadAllowed, isWriteAllowed);
      }

      LOGGER.info("Project created successfully");
    } finally {
      if (orgResourceId != null) {
        switch (resourceType) {
          case PROJECT:
            deleteProject(orgResourceId);
            break;
          case DATASET:
            deleteDataset(orgResourceId);
            break;
          case REPOSITORY:
          default:
            deleteRepository(orgResourceId);
            break;
        }
      }

      DeleteOrganization.Response deleteOrganization =
          organizationServiceBlockingStub.deleteOrganization(
              DeleteOrganization.newBuilder().setOrgId(organization.getId()).build());
      assertTrue(deleteOrganization.getStatus());
    }

    LOGGER.info("Global organization Project test stop................................");
  }

  private void checkResourceActions(
      String orgResourceId, Repository repository, boolean isReadAllowed, boolean isWriteAllowed) {
    try {
      switch (resourceType) {
        case PROJECT:
          client2ProjectServiceStub.getProjectById(
              GetProjectById.newBuilder().setId(orgResourceId).build());
          break;
        case DATASET:
          datasetServiceStubClient2.getDatasetById(
              GetDatasetById.newBuilder().setId(orgResourceId).build());
          break;
        case REPOSITORY:
        default:
          repository =
              versioningServiceBlockingStubClient2
                  .getRepository(
                      GetRepositoryRequest.newBuilder()
                          .setId(
                              RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
                          .build())
                  .getRepository();
          break;
      }
      if (!isReadAllowed) {
        fail();
      }
    } catch (StatusRuntimeException e) {
      if (isReadAllowed) {
        throw e;
      }
    }
    try {
      String description = "new-description" + new Date().getTime();
      switch (resourceType) {
        case PROJECT:
          client2ProjectServiceStub.updateProjectDescription(
              UpdateProjectDescription.newBuilder()
                  .setId(orgResourceId)
                  .setDescription(description)
                  .build());
          break;
        case DATASET:
          datasetServiceStubClient2.updateDatasetDescription(
              UpdateDatasetDescription.newBuilder()
                  .setId(orgResourceId)
                  .setDescription(description)
                  .build());
          break;
        case REPOSITORY:
        default:
          versioningServiceBlockingStubClient2.updateRepository(
              SetRepository.newBuilder()
                  .setId(
                      RepositoryIdentification.newBuilder()
                          .setRepoId(Long.parseLong(orgResourceId)))
                  .setRepository(repository.toBuilder().setDescription(description))
                  .build());
          break;
      }
      if (!isWriteAllowed) {
        fail();
      }
    } catch (StatusRuntimeException e) {
      if (isWriteAllowed) {
        throw e;
      }
    }
  }

  private void updateResource(
      Organization organization,
      String orgResourceId,
      String orgResourceName,
      ResourceVisibility resourceVisibility,
      CollaboratorPermissions.Builder customPermission) {
    collaboratorServiceStubClient1.setResource(
        SetResource.newBuilder()
            .setWorkspaceName(organization.getName())
            .setResourceId(orgResourceId)
            .setResourceType(
                ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build())
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setResourceName(orgResourceName)
            .setVisibility(resourceVisibility)
            .setCollaboratorType(customPermission.getCollaboratorType())
            .setCanDeploy(customPermission.getCanDeploy())
            .build());
  }

  private void deleteRepository(String orgResourceId) {
    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder()
                    .setRepoId(Long.parseLong(orgResourceId))
                    .build())
            .build();
    DeleteRepositoryRequest.Response deleteRepositoryResponse =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    LOGGER.info("Repository deleted successfully");
    LOGGER.info(deleteRepositoryResponse.toString());
    assertTrue(deleteRepositoryResponse.getStatus());
  }

  private void deleteDataset(String orgResourceId) {
    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(orgResourceId).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  private void deleteProject(String orgResourceId) {
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(orgResourceId).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
  }

  private Repository createRepository(
      String orgName,
      ResourceVisibility resourceVisibility,
      CollaboratorPermissions.Builder customPermission) {
    Repository repository;
    // Create repository
    String repoName = "repository-" + new Date().getTime();
    SetRepository.Response createRepositoryResponse =
        versioningServiceBlockingStub.createRepository(
            SetRepository.newBuilder()
                .setId(
                    RepositoryIdentification.newBuilder()
                        .setNamedId(
                            RepositoryNamedIdentification.newBuilder()
                                .setName(repoName)
                                .setWorkspaceName(orgName)))
                .setRepository(
                    Repository.newBuilder()
                        .setVisibility(resourceVisibility)
                        .setName(repoName)
                        .setCustomPermission(customPermission))
                .build());
    repository = createRepositoryResponse.getRepository();
    return repository;
  }

  private Map.Entry<String, String> createDataset(
      Organization organization,
      ResourceVisibility resourceVisibility,
      CollaboratorPermissions.Builder customPermission) {
    // Create dataset
    CreateDataset createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("dataset-" + new Date().getTime());
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .setWorkspaceName(organization.getName())
            .setVisibility(resourceVisibility)
            .setCustomPermission(customPermission)
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset resource = createDatasetResponse.getDataset();
    return new AbstractMap.SimpleEntry<>(resource.getId(), resource.getName());
  }

  private Map.Entry<String, String> createProject(
      Organization organization,
      ResourceVisibility resourceVisibility,
      CollaboratorPermissions.Builder customPermission) {
    // Create project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .setWorkspaceName(organization.getName())
            .setVisibility(resourceVisibility)
            .setCustomPermission(customPermission)
            .build();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project resource = createProjectResponse.getProject();
    return new AbstractMap.SimpleEntry<>(resource.getId(), resource.getName());
  }
}
