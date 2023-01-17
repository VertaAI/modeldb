package ai.verta.modeldb.comment;

import ai.verta.modeldb.Comment;
import ai.verta.modeldb.EntityComment;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.UserCommentEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.UserInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommentDAORdbImpl implements CommentDAO {

  private static final Logger LOGGER = LogManager.getLogger(CommentDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final UACApisUtil uacApisUtil;
  // queries
  private static final String GET_ENTITY_COMMENT_QUERY =
      new StringBuilder("From UserCommentEntity c where c.commentEntity.")
          .append(ModelDBConstants.ENTITY_ID)
          .append(" = :entityId AND c.commentEntity.")
          .append(ModelDBConstants.ENTITY_NAME)
          .append(" =:entityName order by c.")
          .append(ModelDBConstants.DATE_TIME)
          .append(" ASC")
          .toString();
  private static final String ADD_ENTITY_COMMENT_QUERY =
      new StringBuilder("From CommentEntity c where c.")
          .append(ModelDBConstants.ENTITY_ID)
          .append(" = :entityId AND c.")
          .append(ModelDBConstants.ENTITY_NAME)
          .append(" =:entityName")
          .toString();
  private static final String DELETE_USER_COMMENTS_QUERY =
      new StringBuilder("delete from UserCommentEntity uc where ").append("uc.id = :id").toString();

  public CommentDAORdbImpl(UACApisUtil uacApisUtil) {
    this.uacApisUtil = uacApisUtil;
  }

  @Override
  public Comment addComment(String entityType, String entityId, Comment newComment) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(ADD_ENTITY_COMMENT_QUERY);
      query.setParameter("entityId", entityId);
      query.setParameter("entityName", entityType);
      var commentEntity = (CommentEntity) query.uniqueResult();

      if (commentEntity == null) {
        var entityCommentBuilder =
            EntityComment.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setEntityId(entityId)
                .setEntityName(entityType)
                .addComments(newComment);
        commentEntity = RdbmsUtils.generateCommentEntity(entityCommentBuilder.build());
      } else {
        var newUserCommentEntity = new UserCommentEntity(commentEntity, newComment);
        commentEntity.getComments().add(newUserCommentEntity);
      }
      var transaction = session.beginTransaction();
      session.saveOrUpdate(commentEntity);
      transaction.commit();
      LOGGER.debug("Comment inserted successfully");
      return newComment;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addComment(entityType, entityId, newComment);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Comment updateComment(String entityType, String entityId, Comment updatedComment) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var userCommentEntity = session.load(UserCommentEntity.class, updatedComment.getId());
      if (userCommentEntity == null) {
        throw new NotFoundException("Comment does not exist in database");
      }
      userCommentEntity.setUser_id(updatedComment.getUserId());
      userCommentEntity.setOwner(updatedComment.getVertaId());
      userCommentEntity.setMessage(updatedComment.getMessage());
      userCommentEntity.setDate_time(updatedComment.getDateTime());
      var transaction = session.beginTransaction();
      session.update(userCommentEntity);
      transaction.commit();
      LOGGER.debug("Comment updated successfully");
      return userCommentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateComment(entityType, entityId, updatedComment);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Comment> getComments(String entityType, String entityId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_ENTITY_COMMENT_QUERY);
      query.setParameter("entityId", entityId);
      query.setParameter("entityName", entityType);
      List<UserCommentEntity> userCommentEntities = query.list();

      if (userCommentEntities == null) {
        return new ArrayList<>();
      }
      LOGGER.debug("Got {} comments", userCommentEntities.size());
      return RdbmsUtils.convertUserCommentListFromUserComments(userCommentEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getComments(entityType, entityId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Boolean deleteComment(
      String entityType, String entityId, String commentId, UserInfo userInfo) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String finalQuery = DELETE_USER_COMMENTS_QUERY;
      if (userInfo != null) {
        finalQuery = finalQuery + " AND uc." + ModelDBConstants.OWNER + " = :vertaId";
      }
      var query = session.createQuery(finalQuery);
      query.setParameter("id", commentId);
      if (userInfo != null) {
        query.setParameter("vertaId", uacApisUtil.getVertaIdFromUserInfo(userInfo));
      }
      var transaction = session.beginTransaction();
      query.executeUpdate();
      transaction.commit();
      LOGGER.debug("Comments deleted successfully");
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteComment(entityType, entityId, commentId, userInfo);
      } else {
        throw ex;
      }
    }
  }
}
