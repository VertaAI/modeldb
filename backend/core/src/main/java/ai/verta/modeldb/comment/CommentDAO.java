package ai.verta.modeldb.comment;

import ai.verta.modeldb.Comment;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;

public interface CommentDAO {

  /**
   * Insert Comment in database.
   *
   * @param String entityType --> like project, experiment, experimentRun etc.
   * @param String entityId --> like project.id, experiment.id, experimentRun.id etc.
   * @param Comment newComment
   * @return Comment comment --> return the comment
   */
  Comment addComment(String entityType, String entityId, Comment newComment)
      throws InvalidProtocolBufferException;

  /**
   * Update comment in database and return the updated comment to client
   *
   * @param String entityType --> like project, experiment, experimentRun etc.
   * @param String entityId --> like project.id, experiment.id, experimentRun.id etc.
   * @param Comment updatedComment
   * @return Comment comment --> return the single updated comment
   */
  Comment updateComment(String entityType, String entityId, Comment updatedComment)
      throws InvalidProtocolBufferException;

  /**
   * Get All entity comments from database.
   *
   * @param String entityType --> like project, experiment, experimentRun etc.
   * @param String entityId --> like project.id, experiment.id, experimentRun.id etc.
   * @return List<Comment> commentList
   */
  List<Comment> getComments(String entityType, String entityId)
      throws InvalidProtocolBufferException;

  /**
   * Delete the selected comment from EntityComment.
   *
   * @param String entityType --> like project, experiment, experimentRun etc.
   * @param String entityId --> like project.id, experiment.id, experimentRun.id etc.
   * @param String commentId --> like comment.id
   * @param UserInfo userInfo
   * @return Boolean deletedStatus
   */
  Boolean deleteComment(String entityType, String entityId, String commentId, UserInfo userInfo)
      throws InvalidProtocolBufferException;
}
