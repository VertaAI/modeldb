package ai.verta.modeldb.project;

import ai.verta.modeldb.*;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.utils.InternalFutureGrpc;
import io.grpc.stub.StreamObserver;
import java.util.Collections;

public class FutureProjectServiceImpl extends ProjectServiceImplBase {
  private final FutureExecutor executor;
  private final FutureProjectDAO futureProjectDAO;

  public FutureProjectServiceImpl(DAOSet daoSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
  }

  @Override
  public void createProject(
      CreateProject request, StreamObserver<CreateProject.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .createProject(request)
              .thenApply(
                  createdProject ->
                      CreateProject.Response.newBuilder().setProject(createdProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectDescription(
      UpdateProjectDescription request,
      StreamObserver<UpdateProjectDescription.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .updateProjectDescription(request)
              .thenApply(
                  updatedProject ->
                      UpdateProjectDescription.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectAttributes(
      AddProjectAttributes request,
      StreamObserver<AddProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .logAttributes(
                  LogAttributes.newBuilder()
                      .setId(request.getId())
                      .addAllAttributes(request.getAttributesList())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      AddProjectAttributes.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectAttributes(
      UpdateProjectAttributes request,
      StreamObserver<UpdateProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .updateProjectAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      UpdateProjectAttributes.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .getAttributes(request)
              .thenApply(
                  attributes ->
                      GetAttributes.Response.newBuilder().addAllAttributes(attributes).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectAttributes(
      DeleteProjectAttributes request,
      StreamObserver<DeleteProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .deleteAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectAttributes.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTags(
      AddProjectTags request, StreamObserver<AddProjectTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .addTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      AddProjectTags.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getTags(request)
              .thenApply(tags -> GetTags.Response.newBuilder().addAllTags(tags).build(), executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTags(
      DeleteProjectTags request, StreamObserver<DeleteProjectTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectTags.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTag(
      AddProjectTag request, StreamObserver<AddProjectTag.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .addTags(
                  AddProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      AddProjectTag.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTag(
      DeleteProjectTag request, StreamObserver<DeleteProjectTag.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteTags(
                  DeleteProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .setDeleteAll(false)
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectTag.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProject(
      DeleteProject request, StreamObserver<DeleteProject.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteProjects(Collections.singletonList(request.getId()))
              .thenApply(
                  project -> DeleteProject.Response.newBuilder().setStatus(true).build(), executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjects(
      GetProjects request, StreamObserver<GetProjects.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .findProjects(
                  FindProjects.newBuilder()
                      .setWorkspaceName(request.getWorkspaceName())
                      .setAscending(request.getAscending())
                      .setSortKey(request.getSortKey())
                      .setPageNumber(request.getPageNumber())
                      .setPageLimit(request.getPageLimit())
                      .build())
              .thenApply(
                  findProjectResponse ->
                      GetProjects.Response.newBuilder()
                          .addAllProjects(findProjectResponse.getProjectsList())
                          .setTotalRecords(findProjectResponse.getTotalRecords())
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectById(
      GetProjectById request, StreamObserver<GetProjectById.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getProjectById(request.getId())
              .thenApply(
                  project -> GetProjectById.Response.newBuilder().setProject(project).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectByName(
      GetProjectByName request, StreamObserver<GetProjectByName.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getProjectByName(request);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void verifyConnection(
      Empty request, StreamObserver<VerifyConnectionResponse> responseObserver) {
    try {
      final var response = futureProjectDAO.verifyConnection();
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deepCopyProject(
      DeepCopyProject request, StreamObserver<DeepCopyProject.Response> responseObserver) {
    super.deepCopyProject(request, responseObserver);
  }

  @Override
  public void getSummary(GetSummary request, StreamObserver<GetSummary.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getSummary(request);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectReadme(
      SetProjectReadme request, StreamObserver<SetProjectReadme.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .setProjectReadme(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      SetProjectReadme.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectReadme(
      GetProjectReadme request, StreamObserver<GetProjectReadme.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectReadme(request);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectShortName(
      SetProjectShortName request, StreamObserver<SetProjectShortName.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .setProjectShortName(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      SetProjectShortName.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectShortName(
      GetProjectShortName request, StreamObserver<GetProjectShortName.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectShortName(request);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logProjectCodeVersion(
      LogProjectCodeVersion request,
      StreamObserver<LogProjectCodeVersion.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .logProjectCodeVersion(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      LogProjectCodeVersion.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectCodeVersion(
      GetProjectCodeVersion request,
      StreamObserver<GetProjectCodeVersion.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectCodeVersion(request);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findProjects(
      FindProjects request, StreamObserver<FindProjects.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.findProjects(request);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getUrlForArtifact(request);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogProjectArtifacts request, StreamObserver<LogProjectArtifacts.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .logArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      LogProjectArtifacts.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .getArtifacts(request)
              .thenApply(
                  artifacts ->
                      GetArtifacts.Response.newBuilder().addAllArtifacts(artifacts).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteProjectArtifact request,
      StreamObserver<DeleteProjectArtifact.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .deleteArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectArtifact.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjects(
      DeleteProjects request, StreamObserver<DeleteProjects.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteProjects(request.getIdsList())
              .thenApply(
                  project -> DeleteProjects.Response.newBuilder().setStatus(true).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectDatasetCount(
      GetProjectDatasetCount request,
      StreamObserver<GetProjectDatasetCount.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getProjectDatasetCount(request.getProjectId())
              .thenApply(
                  datasetCount ->
                      GetProjectDatasetCount.Response.newBuilder()
                          .setDatasetCount(datasetCount)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
