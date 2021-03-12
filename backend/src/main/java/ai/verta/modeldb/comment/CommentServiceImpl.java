package ai.verta.modeldb.comment;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddComment;
import ai.verta.modeldb.Comment;
import ai.verta.modeldb.CommentServiceGrpc.CommentServiceImplBase;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeleteComment;
import ai.verta.modeldb.GetComments;
import ai.verta.modeldb.GetComments.Response;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.UpdateComment;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.UserInfo;
import io.grpc.stub.StreamObserver;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommentServiceImpl extends CommentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(CommentServiceImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final CommentDAO commentDAO;
  private final ExperimentRunDAO experimentRunDAO;
  private final AuditLogLocalDAO auditLogLocalDAO;
  private static final String SERVICE_NAME =
      String.format("%s.%s", ModelDBConstants.SERVICE_NAME, ModelDBConstants.COMMENT);

  String experimentRunEntity = ExperimentRunEntity.class.getSimpleName();

  public CommentServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.authService = serviceSet.authService;
    this.roleService = serviceSet.roleService;
    this.commentDAO = daoSet.commentDAO;
    this.experimentRunDAO = daoSet.experimentRunDAO;
    this.auditLogLocalDAO = daoSet.auditLogLocalDAO;
  }

  private void saveAuditLog(
      Optional<UserInfo> userInfo,
      ModelDBServiceActions action,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Map<String, Long> resourceIdWorkspaceIdMap,
      String request,
      String response) {
    auditLogLocalDAO.saveAuditLog(
        new AuditLogLocalEntity(
            SERVICE_NAME,
            authService.getVertaIdFromUserInfo(
                userInfo.orElseGet(authService::getCurrentLoginUserInfo)),
            action,
            resourceIdWorkspaceIdMap,
            modelDBServiceResourceTypes,
            ServiceEnum.Service.MODELDB_SERVICE,
            MonitoringInterceptor.METHOD_NAME.get(),
            request,
            response));
  }

  /**
   * Convert AddComment request to Comment object. This method generates the Comment Id using UUID
   * and puts it in the Comment object.
   *
   * @param request : AddComment
   * @param userInfo : UserInfo
   * @return Comment comment
   */
  private Comment getCommentFromRequest(AddComment request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getEntityId().isEmpty() && request.getMessage().isEmpty()) {
      errorMessage = "Entity ID and comment message not found in AddComment request";
    } else if (request.getEntityId().isEmpty()) {
      errorMessage = "Entity ID not found in AddComment request";
    } else if (request.getMessage().isEmpty()) {
      errorMessage = "Comment message not found in AddComment request";
    }

    if (errorMessage != null) {
      throw new InvalidArgumentException(errorMessage);
    }

    return Comment.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setUserId(userInfo != null ? userInfo.getUserId() : "")
        .setVertaId(authService.getVertaIdFromUserInfo(userInfo))
        .setDateTime(Calendar.getInstance().getTimeInMillis())
        .setMessage(request.getMessage())
        .build();
  }

  private Comment getUpdatedCommentFromRequest(UpdateComment request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getEntityId().isEmpty()
        && request.getMessage().isEmpty()
        && request.getId().isEmpty()) {
      errorMessage =
          "Entity ID and Comment message and Comment ID not found in UpdateComment request";
    } else if (request.getEntityId().isEmpty()) {
      errorMessage = "Entity ID not found in UpdateComment request";
    } else if (request.getMessage().isEmpty()) {
      errorMessage = "Comment message not found in UpdateComment request";
    } else if (request.getId().isEmpty()) {
      errorMessage = "Comment ID is not found in UpdateComment request";
    }

    if (errorMessage != null) {
      throw new InvalidArgumentException(errorMessage);
    }

    return Comment.newBuilder()
        .setId(request.getId())
        .setUserId(userInfo != null ? userInfo.getUserId() : "")
        .setVertaId(authService.getVertaIdFromUserInfo(userInfo))
        .setDateTime(Calendar.getInstance().getTimeInMillis())
        .setMessage(request.getMessage())
        .build();
  }

  @Override
  public void addExperimentRunComment(
      AddComment request, StreamObserver<AddComment.Response> responseObserver) {
    try {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      Comment comment = getCommentFromRequest(request, userInfo);
      Comment newComment =
          commentDAO.addComment(experimentRunEntity, request.getEntityId(), comment);
      AddComment.Response response =
          AddComment.Response.newBuilder().setComment(newComment).build();

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getEntityId());
      GetResourcesResponseItem responseItem =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.of(userInfo),
          ModelDBServiceActions.CREATE,
          ModelDBServiceResourceTypes.EXPERIMENT_RUN,
          Collections.singletonMap(newComment.getId(), responseItem.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, AddComment.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateExperimentRunComment(
      UpdateComment request, StreamObserver<UpdateComment.Response> responseObserver) {
    try {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      Comment updatedComment = getUpdatedCommentFromRequest(request, userInfo);
      updatedComment =
          commentDAO.updateComment(experimentRunEntity, request.getEntityId(), updatedComment);
      UpdateComment.Response response =
          UpdateComment.Response.newBuilder().setComment(updatedComment).build();
      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getEntityId());
      GetResourcesResponseItem responseItem =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.of(userInfo),
          ModelDBServiceActions.UPDATE,
          ModelDBServiceResourceTypes.EXPERIMENT_RUN,
          Collections.singletonMap(updatedComment.getId(), responseItem.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response));
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, UpdateComment.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunComments(
      GetComments request, StreamObserver<Response> responseObserver) {
    try {
      if (request.getEntityId().isEmpty()) {
        String errorMessage = "Entity ID not found in GetComments request";
        throw new InvalidArgumentException(errorMessage);
      }
      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getEntityId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Comment> comments = commentDAO.getComments(experimentRunEntity, request.getEntityId());
      GetComments.Response response =
          GetComments.Response.newBuilder().addAllComments(comments).build();
      GetResourcesResponseItem responseItem =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      Map<String, Long> auditResourceMap = new HashMap<>();
      comments.forEach(
          comment -> auditResourceMap.put(comment.getId(), responseItem.getWorkspaceId()));
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          ModelDBServiceResourceTypes.EXPERIMENT_RUN,
          auditResourceMap,
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetComments.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRunComment(
      DeleteComment request, StreamObserver<DeleteComment.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getEntityId().isEmpty() && request.getId().isEmpty()) {
        errorMessage = "Entity ID and Comment ID not found in DeleteComment request";
      } else if (request.getEntityId().isEmpty()) {
        errorMessage = "Entity ID not found in DeleteComment request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Comment ID not found in DeleteComment request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      Boolean status =
          commentDAO.deleteComment(
              experimentRunEntity, request.getEntityId(), request.getId(), userInfo);
      DeleteComment.Response response =
          DeleteComment.Response.newBuilder().setStatus(status).build();
      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getEntityId());
      GetResourcesResponseItem responseItem =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.of(userInfo),
          ModelDBServiceActions.DELETE,
          ModelDBServiceResourceTypes.EXPERIMENT_RUN,
          Collections.singletonMap(request.getId(), responseItem.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteComment.Response.getDefaultInstance());
    }
  }
}
