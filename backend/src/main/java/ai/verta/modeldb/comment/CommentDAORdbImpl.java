package ai.verta.modeldb.comment;

import ai.verta.modeldb.Comment;
import ai.verta.modeldb.EntityComment;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.UserCommentEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.UserInfo;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class CommentDAORdbImpl implements CommentDAO {

  private static final Logger LOGGER = LogManager.getLogger(CommentDAORdbImpl.class);
  private final AuthService authService;
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

  public CommentDAORdbImpl(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public Comment addComment(String entityType, String entityId, Comment newComment) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(ADD_ENTITY_COMMENT_QUERY);
      query.setParameter("entityId", entityId);
      query.setParameter("entityName", entityType);
      CommentEntity commentEntity = (CommentEntity) query.uniqueResult();

      if (commentEntity == null) {
        EntityComment.Builder entityCommentBuilder =
            EntityComment.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setEntityId(entityId)
                .setEntityName(entityType)
                .addComments(newComment);
        commentEntity = RdbmsUtils.generateCommentEntity(entityCommentBuilder.build());
      } else {
        UserCommentEntity newUserCommentEntity = new UserCommentEntity(commentEntity, newComment);
        commentEntity.getComments().add(newUserCommentEntity);
      }
      session.saveOrUpdate(commentEntity);
      transaction.commit();
      LOGGER.debug("Comment inserted successfully");
      return newComment;
    }
  }

  @Override
  public Comment updateComment(String entityType, String entityId, Comment updatedComment) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      UserCommentEntity userCommentEntity =
          session.load(UserCommentEntity.class, updatedComment.getId());
      if (userCommentEntity == null) {
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Comment does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      userCommentEntity.setUser_id(updatedComment.getUserId());
      userCommentEntity.setOwner(updatedComment.getVertaId());
      userCommentEntity.setMessage(updatedComment.getMessage());
      userCommentEntity.setDate_time(updatedComment.getDateTime());
      session.update(userCommentEntity);
      LOGGER.debug("Comment updated successfully");
      transaction.commit();
      return userCommentEntity.getProtoObject();
    }
  }

  @Override
  public List<Comment> getComments(String entityType, String entityId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_ENTITY_COMMENT_QUERY);
      query.setParameter("entityId", entityId);
      query.setParameter("entityName", entityType);
      List<UserCommentEntity> userCommentEntities = query.list();

      if (userCommentEntities == null) {
        return new ArrayList<>();
      }
      LOGGER.debug("Got {} comments", userCommentEntities.size());
      return RdbmsUtils.convertUserCommentListFromUserComments(userCommentEntities);
    }
  }

  @Override
  public Boolean deleteComment(
      String entityType, String entityId, String commentId, UserInfo userInfo) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      String finalQuery = DELETE_USER_COMMENTS_QUERY;
      if (userInfo != null) {
        finalQuery = finalQuery + " AND uc." + ModelDBConstants.OWNER + " = :vertaId";
      }
      Query query = session.createQuery(finalQuery);
      query.setParameter("id", commentId);
      if (userInfo != null) {
        query.setParameter("vertaId", authService.getVertaIdFromUserInfo(userInfo));
      }
      query.executeUpdate();
      transaction.commit();
      LOGGER.debug("Comments deleted successfully");
      return true;
    }
  }
}
