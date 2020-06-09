package ai.verta.modeldb.comment;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddComment;
import ai.verta.modeldb.Comment;
import ai.verta.modeldb.CommentServiceGrpc.CommentServiceImplBase;
import ai.verta.modeldb.DeleteComment;
import ai.verta.modeldb.GetComments;
import ai.verta.modeldb.GetComments.Response;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.UpdateComment;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
      LOGGER.info(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(AddComment.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
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
      LOGGER.info(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(UpdateComment.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
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
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getEntityId().isEmpty()) {
        String errorMessage = "Entity ID not found in GetComments request";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetComments.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getEntityId().isEmpty() && request.getId().isEmpty()) {
        errorMessage = "Entity ID and Comment ID not found in DeleteComment request";
      } else if (request.getEntityId().isEmpty()) {
        errorMessage = "Entity ID not found in DeleteComment request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Comment ID not found in DeleteComment request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteComment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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
