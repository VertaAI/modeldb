package ai.verta.modeldb.experiment;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.AddAttributes;
import ai.verta.modeldb.AddExperimentAttributes;
import ai.verta.modeldb.AddExperimentTag;
import ai.verta.modeldb.AddExperimentTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.ArtifactTypeEnum.ArtifactType;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DeleteExperiment;
import ai.verta.modeldb.DeleteExperimentArtifact;
import ai.verta.modeldb.DeleteExperimentAttributes;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.DeleteExperimentTag;
import ai.verta.modeldb.DeleteExperimentTags;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetExperimentById;
import ai.verta.modeldb.GetExperimentByName;
import ai.verta.modeldb.GetExperimentCodeVersion;
import ai.verta.modeldb.GetExperimentsInProject;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogExperimentArtifacts;
import ai.verta.modeldb.LogExperimentCodeVersion;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.UpdateExperimentDescription;
import ai.verta.modeldb.UpdateExperimentName;
import ai.verta.modeldb.UpdateExperimentNameOrDescription;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private ExperimentDAO experimentDAO;
  private ProjectDAO projectDAO;
  private ArtifactStoreDAO artifactStoreDAO;

  public ExperimentServiceImpl(
      AuthService authService,
      RoleService roleService,
      ExperimentDAO experimentDAO,
      ProjectDAO projectDAO,
      ArtifactStoreDAO artifactStoreDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.experimentDAO = experimentDAO;
    this.projectDAO = projectDAO;
    this.artifactStoreDAO = artifactStoreDAO;
  }

  /**
   * Convert CreateExperiment request to Experiment object. This method generate the experiment Id
   * using UUID and put it in Experiment object.
   *
   * @param request : CreateExperiment request
   * @param userInfo : current login UserInfo
   * @return Experiment : experimentDeleteExperiments
   */
  private Experiment getExperimentFromRequest(CreateExperiment request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getProjectId().isEmpty() && request.getName().isEmpty()) {
      errorMessage = "Project ID and Experiment name not found in CreateExperiment request";
    } else if (request.getProjectId().isEmpty()) {
      errorMessage = "Project ID not found in CreateExperiment request";
    } else if (request.getName().isEmpty()) {
      errorMessage = "Experiment name not found in CreateExperiment request";
    }

    if (errorMessage != null) {
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateExperiment.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    /*
     * Create Experiment entity from given CreateExperiment request. generate UUID and put as id in
     * Experiment for uniqueness.
     */
    Experiment.Builder experimentBuilder =
        Experiment.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllArtifacts(request.getArtifactsList());

    if (App.getInstance().getStoreClientCreationTimestamp() && request.getDateCreated() != 0L) {
      experimentBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }
    if (userInfo != null) {
      experimentBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    return experimentBuilder.build();
  }

  /**
   * Convert CreateExperiment request to Experiment entity and insert in database.
   *
   * @param request : CreateExperiment request
   * @param responseObserver : CreateExperiment.Response responseObserver
   */
  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      Experiment experiment = getExperimentFromRequest(request, userInfo);

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.UPDATE);

      experiment = experimentDAO.insertExperiment(experiment);
      responseObserver.onNext(
          CreateExperiment.Response.newBuilder().setExperiment(experiment).build());
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
              .addDetails(Any.pack(CreateExperiment.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetExperimentsInProject request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentsInProject.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.getExperimentsInProject(
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      responseObserver.onNext(
          GetExperimentsInProject.Response.newBuilder()
              .addAllExperiments(experimentPaginationDTO.getExperiments())
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build());
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
              .addDetails(Any.pack(GetExperimentsInProject.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentById request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment experiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.READ);

      responseObserver.onNext(
          GetExperimentById.Response.newBuilder().setExperiment(experiment).build());
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
              .addDetails(Any.pack(GetExperimentById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      String errorMessage = null;
      if (request.getProjectId().isEmpty() || request.getName().isEmpty()) {
        errorMessage = "Experiment name and Project ID is not found in GetExperimentByName request";
      } else if (request.getProjectId().isEmpty()) {
        errorMessage = "Project ID not found in GetExperimentByName request";
      } else if (request.getName().isEmpty()) {
        errorMessage = "Experiment name not found in GetExperimentByName request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      List<KeyValue> keyValue = new ArrayList<>();
      Value projectIdValue = Value.newBuilder().setStringValue(request.getProjectId()).build();
      Value nameValue = Value.newBuilder().setStringValue(request.getName()).build();
      keyValue.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.PROJECT_ID)
              .setValue(projectIdValue)
              .build());
      keyValue.add(KeyValue.newBuilder().setKey(ModelDBConstants.NAME).setValue(nameValue).build());

      List<Experiment> experiments = experimentDAO.getExperiments(keyValue);
      if (experiments == null || experiments.isEmpty()) {
        errorMessage =
            "Experiment with name " + nameValue + " not found in project " + projectIdValue;
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
                .build();
        StatusRuntimeException statusRuntimeException =
            StatusProto.toStatusRuntimeException(status);
        ErrorCountResource.inc(statusRuntimeException);
        responseObserver.onError(statusRuntimeException);
        return;
      }
      if (experiments.size() != 1) {
        errorMessage =
            "Multiple experiments with name " + nameValue + " found in project " + projectIdValue;
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
                .build();
        StatusRuntimeException statusRuntimeException =
            StatusProto.toStatusRuntimeException(status);
        ErrorCountResource.inc(statusRuntimeException);
        responseObserver.onError(statusRuntimeException);
        return;
      }
      responseObserver.onNext(
          GetExperimentByName.Response.newBuilder().setExperiment(experiments.get(0)).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * Update Experiment name Or Description in Experiment Entity. Create Experiment object with
   * updated data from UpdateExperimentNameOrDescription request and update in database.
   *
   * @param request : UpdateExperimentNameOrDescription request
   * @param responseObserver : UpdateExperimentNameOrDescription.Response responseObserver
   */
  @Override
  public void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage =
            "Experiment ID not found in UpdateExperimentNameOrDescription request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(
                    Any.pack(UpdateExperimentNameOrDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment = null;
      if (!request.getName().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentName(
                request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));
      }
      if (!request.getDescription().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());
      }

      responseObserver.onNext(
          UpdateExperimentNameOrDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build());
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
              .addDetails(Any.pack(UpdateExperimentNameOrDescription.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * Update Experiment name in Experiment Entity. Create Experiment object with updated data from
   * UpdateExperimentName request and update in database.
   *
   * @param request : UpdateExperimentName request
   * @param responseObserver : UpdateExperimentName.Response responseObserver
   */
  @Override
  public void updateExperimentName(
      UpdateExperimentName request,
      StreamObserver<UpdateExperimentName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in UpdateExperimentName request";
      } else if (request.getName().isEmpty()) {
        errorMessage = "Experiment name not found in UpdateExperimentName request";
      }

      if (errorMessage != null) {
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(UpdateExperimentName.getDefaultInstance()));
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.updateExperimentName(
              request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      responseObserver.onNext(
          UpdateExperimentName.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(UpdateExperimentName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * Update Experiment Description in Experiment Entity. Create Experiment object with updated data
   * from UpdateExperimentDescription request and update in database.
   *
   * @param request : UpdateExperimentDescription request
   * @param responseObserver : UpdateExperimentDescription.Response responseObserver
   */
  @Override
  public void updateExperimentDescription(
      UpdateExperimentDescription request,
      StreamObserver<UpdateExperimentDescription.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in UpdateExperimentDescription request";
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(UpdateExperimentDescription.getDefaultInstance()));
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());

      responseObserver.onNext(
          UpdateExperimentDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build());
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
              .addDetails(Any.pack(UpdateExperimentDescription.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty()) {
        errorMessage = "Experiment ID and Experiment tags not found in AddExperimentTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentTags request";
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "Experiment tags not found in AddExperimentTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      responseObserver.onNext(
          AddExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(AddExperimentTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentTag(
      AddExperimentTag request, StreamObserver<AddExperimentTag.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Experiment ID and Experiment Tag not found in AddExperimentTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Experiment Tag not found in AddExperimentTag request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentTag.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      responseObserver.onNext(
          AddExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(AddExperimentTag.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetTags request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<String> experimentTags = experimentDAO.getExperimentTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(experimentTags).build());
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
              .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentTags(
      DeleteExperimentTags request,
      StreamObserver<DeleteExperimentTags.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Experiment ID and Experiment tags not found in DeleteExperimentTags request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentTags request";
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Experiment tags not found in DeleteExperimentTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(DeleteExperimentTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentTag(
      DeleteExperimentTag request, StreamObserver<DeleteExperimentTag.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Experiment ID and Experiment tag not found in DeleteExperimentTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Experiment tag not found in DeleteExperimentTag request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentTag.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), Collections.singletonList(request.getTag()), false);
      responseObserver.onNext(
          DeleteExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(DeleteExperimentTag.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in AddAttributes request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        String errorMessage =
            "Access is denied. Experiment not found for given id : " + request.getId();
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(AddAttributes.getDefaultInstance()));
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentDAO.addExperimentAttributes(
          request.getId(), Collections.singletonList(request.getAttribute()));
      responseObserver.onNext(AddAttributes.Response.newBuilder().setStatus(true).build());
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
              .addDetails(Any.pack(AddAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentAttributes(
      AddExperimentAttributes request,
      StreamObserver<AddExperimentAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage =
            "Experiment ID and Experiment Attributes not found in AddExperimentAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Experiment Attributes not found in AddExperimentAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment experiment =
          experimentDAO.addExperimentAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddExperimentAttributes.Response.newBuilder().setExperiment(experiment).build());
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
              .addDetails(Any.pack(AddExperimentAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage =
            "Experiment ID and Experiment Attribute keys not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in GetAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "Experiment Attribute keys not found in GetAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        errorMessage = "Access is denied. Experiment not found for given id : " + request.getId();
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(GetAttributes.getDefaultInstance()));
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          experimentDAO.getExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      responseObserver.onNext(
          GetAttributes.Response.newBuilder().addAllAttributes(attributes).build());
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
              .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "Experiment ID and Experiment Attribute keys not found in DeleteExperimentAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Experiment Attribute keys not found in DeleteExperimentAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentAttributes.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build());
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
              .addDetails(Any.pack(DeleteExperimentAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in DeleteExperiment request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean deleteStatus = deleteExperiments(Collections.singletonList(request.getId()));
      responseObserver.onNext(
          DeleteExperiment.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logExperimentCodeVersion(
      LogExperimentCodeVersion request,
      StreamObserver<LogExperimentCodeVersion.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getCodeVersion() == null) {
        errorMessage =
            "Experiment ID and Code version not found in LogExperimentCodeVersion request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in LogExperimentCodeVersion request";
      } else if (request.getCodeVersion() == null) {
        errorMessage = "CodeVersion not found in LogExperimentCodeVersion request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogExperimentCodeVersion.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      /*User validation*/
      Experiment existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperiment.getProjectId(),
          ModelDBServiceActions.UPDATE);

      /*UpdateCode version*/
      Experiment updatedExperiment;

      if (!existingExperiment.getCodeVersionSnapshot().hasCodeArchive()
          && !existingExperiment.getCodeVersionSnapshot().hasGitSnapshot()) {
        updatedExperiment =
            experimentDAO.logExperimentCodeVersion(request.getId(), request.getCodeVersion());
      } else {
        errorMessage = "Code version already logged for experiment " + existingExperiment.getId();
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogExperimentCodeVersion.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      /*Build response*/
      LogExperimentCodeVersion.Response.Builder responseBuilder =
          LogExperimentCodeVersion.Response.newBuilder().setExperiment(updatedExperiment);
      responseObserver.onNext(responseBuilder.build());
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
              .addDetails(Any.pack(LogExperimentCodeVersion.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentCodeVersion(
      GetExperimentCodeVersion request,
      StreamObserver<GetExperimentCodeVersion.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentCodeVersion request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentCodeVersion.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      /*User validation*/
      Experiment existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperiment.getProjectId(),
          ModelDBServiceActions.READ);

      /*Get code version*/
      CodeVersion codeVersion = existingExperiment.getCodeVersionSnapshot();

      responseObserver.onNext(
          GetExperimentCodeVersion.Response.newBuilder().setCodeVersion(codeVersion).build());
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
              .addDetails(Any.pack(GetExperimentCodeVersion.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * For getting experiments that user has access to (either as owner or a collaborator), fetch all
   * experiments of the requested experimentIds then iterate that list and check if experiment is
   * accessible or not. The list of accessible experimentIDs is built and returned by this method.
   *
   * @param requestedExperimentIds : experiment Ids
   * @return List<String> : list of accessible Experiment Id
   */
  public List<String> getAccessibleExperimentIDs(
      List<String> requestedExperimentIds, ModelDBServiceActions modelDBServiceActions) {
    Map<String, String> projectIdExperimentIdMap =
        experimentDAO.getProjectIdsByExperimentIds(requestedExperimentIds);
    Set<String> projectIdSet = new HashSet<>(projectIdExperimentIdMap.values());

    List<String> accessibleExperimentIds = new ArrayList<>();
    List<String> allowedProjectIds;
    // Validate if current user has access to the entity or not
    if (projectIdSet.size() == 1) {
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT,
          modelDBServiceActions,
          new ArrayList<>(projectIdSet).get(0));
      accessibleExperimentIds.addAll(requestedExperimentIds);
    } else {
      allowedProjectIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedProjectIds.retainAll(requestedExperimentIds);
      accessibleExperimentIds.addAll(allowedProjectIds);
    }
    return accessibleExperimentIds;
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty() && request.getExperimentIdsList().isEmpty()) {
        String errorMessage = "Project ID and Experiment Id's not found in FindExperiments request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindExperiments.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      if (!request.getExperimentIdsList().isEmpty()) {
        List<String> accessibleExperimentIds =
            getAccessibleExperimentIDs(request.getExperimentIdsList(), ModelDBServiceActions.READ);
        if (accessibleExperimentIds.isEmpty()) {
          String errorMessage =
              "Access is denied. User is unauthorized for given Experiment IDs : "
                  + accessibleExperimentIds;
          ModelDBUtils.logAndThrowError(
              errorMessage,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperiments.getDefaultInstance()));
        }
        request =
            request
                .toBuilder()
                .clearExperimentIds()
                .addAllExperimentIds(accessibleExperimentIds)
                .build();
      }

      ExperimentPaginationDTO experimentPaginationDTO = experimentDAO.findExperiments(request);
      responseObserver.onNext(
          FindExperiments.Response.newBuilder()
              .addAllExperiments(experimentPaginationDTO.getExperiments())
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build());
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
              .addDetails(Any.pack(FindExperiments.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getKey().isEmpty()
          && request.getMethod().isEmpty()) {
        errorMessage = "Experiment ID and Key and Method not found in GetUrlForArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in GetUrlForArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact Key not found in GetUrlForArtifact request";
      } else if (request.getMethod().isEmpty()) {
        errorMessage = "Method is not found in GetUrlForArtifact request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String s3Key = null;

      /*Process code*/
      if (request.getArtifactType() == ArtifactType.CODE) {
        // just creating the error string
        errorMessage = "Code versioning artifact not found at experiment and project level";
        s3Key = getUrlForCode(request);
      } else {
        errorMessage = "Experiment level artifacts only supported for code";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (s3Key == null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      GetUrlForArtifact.Response response =
          artifactStoreDAO.getUrlForArtifact(s3Key, request.getMethod());
      responseObserver.onNext(response);
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
              .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private String getUrlForCode(GetUrlForArtifact request) throws InvalidProtocolBufferException {
    String s3Key = null;
    Experiment expr = experimentDAO.getExperiment(request.getId());
    if (expr.getCodeVersionSnapshot() != null
        && expr.getCodeVersionSnapshot().getCodeArchive() != null) {
      s3Key = expr.getCodeVersionSnapshot().getCodeArchive().getPath();
    } else {
      Project proj = projectDAO.getProjectByID(expr.getProjectId());
      if (proj.getCodeVersionSnapshot() != null
          && proj.getCodeVersionSnapshot().getCodeArchive() != null) {
        s3Key = proj.getCodeVersionSnapshot().getCodeArchive().getPath();
      }
    }
    return s3Key;
  }

  @Override
  public void logArtifacts(
      LogExperimentArtifacts request,
      StreamObserver<LogExperimentArtifacts.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getArtifactsList().isEmpty()) {
        errorMessage = "Experiment ID and Artifacts not found in LogArtifacts request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in LogArtifacts request";
      } else if (request.getArtifactsList().isEmpty()) {
        errorMessage = "Artifacts not found in LogArtifacts request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogExperimentArtifacts.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        errorMessage = "Access is denied. Experiment not found for given id : " + request.getId();
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(LogExperimentArtifacts.getDefaultInstance()));
      }

      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());

      Experiment updatedExperiment = experimentDAO.logArtifacts(request.getId(), artifactList);
      LogExperimentArtifacts.Response.Builder responseBuilder =
          LogExperimentArtifacts.Response.newBuilder().setExperiment(updatedExperiment);
      responseObserver.onNext(responseBuilder.build());
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
              .addDetails(Any.pack(LogExperimentArtifacts.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetArtifacts request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        String errorMessage =
            "Access is denied. Experiment not found for given id : " + request.getId();
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(GetArtifacts.getDefaultInstance()));
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> artifactList = experimentDAO.getExperimentArtifacts(request.getId());
      responseObserver.onNext(
          GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build());
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
              .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteExperimentArtifact request,
      StreamObserver<DeleteExperimentArtifact.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getKey().isEmpty()) {
        errorMessage = "Experiment ID and Artifact key not found in DeleteArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in DeleteArtifact request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteArtifacts(request.getId(), request.getKey());
      responseObserver.onNext(
          DeleteExperimentArtifact.Response.newBuilder().setExperiment(updatedExperiment).build());
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
              .addDetails(Any.pack(DeleteExperimentArtifact.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getIdsList().isEmpty()) {
        String errorMessage = "Experiment IDs not found in DeleteExperiments request";
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteExperiment.Response.getDefaultInstance()));
      }

      boolean deleteStatus = deleteExperiments(request.getIdsList());
      responseObserver.onNext(
          DeleteExperiments.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteExperiments.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private boolean deleteExperiments(List<String> experimentIds)
      throws InvalidProtocolBufferException {

    List<String> accessibleExperimentIds =
        getAccessibleExperimentIDs(experimentIds, ModelDBServiceActions.UPDATE);

    if (accessibleExperimentIds.isEmpty()) {
      String errorMessage =
          "Access is denied. User is unauthorized for given Experiment IDs : "
              + accessibleExperimentIds;
      ModelDBUtils.logAndThrowError(
          errorMessage,
          Code.PERMISSION_DENIED_VALUE,
          Any.pack(DeleteExperiments.getDefaultInstance()));
    }

    return experimentDAO.deleteExperiments(accessibleExperimentIds);
  }
}
