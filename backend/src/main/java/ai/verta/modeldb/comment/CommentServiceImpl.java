package ai.verta.modeldb.comment;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.CommentServiceGrpc.CommentServiceImplBase;
import ai.verta.modeldb.GetComments.Response;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.UserInfo;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class CommentServiceImpl extends CommentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(CommentServiceImpl.class);
  private AuthService authService;
  private CommentDAO commentDAO;
  private ExperimentRunDAO experimentRunDAO;
  private RoleService roleService;

  String experimentRunEntity = ExperimentRunEntity.class.getSimpleName();

  public CommentServiceImpl(
      AuthService authService,
      CommentDAO commentDAO,
      ExperimentRunDAO experimentRunDAO,
      RoleService roleService) {
    this.authService = authService;
    this.commentDAO = commentDAO;
    this.experimentRunDAO = experimentRunDAO;
    this.roleService = roleService;
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
      responseObserver.onNext(AddComment.Response.newBuilder().setComment(newComment).build());
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
      responseObserver.onNext(
          UpdateComment.Response.newBuilder().setComment(updatedComment).build());
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
      responseObserver.onNext(GetComments.Response.newBuilder().addAllComments(comments).build());
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
      responseObserver.onNext(DeleteComment.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteComment.Response.getDefaultInstance());
    }
  }
}
