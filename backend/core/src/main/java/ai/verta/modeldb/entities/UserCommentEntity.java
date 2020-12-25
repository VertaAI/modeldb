package ai.verta.modeldb.entities;

import ai.verta.modeldb.Comment;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_comment")
public class UserCommentEntity {

  public UserCommentEntity() {}

  public UserCommentEntity(Object entity, Comment comment) {

    setId(comment.getId());
    setUser_id(comment.getUserId());
    setOwner(comment.getVertaId());
    setDate_time(comment.getDateTime());
    setMessage(comment.getMessage());

    if (entity instanceof CommentEntity) {
      setCommentEntity(entity);
    }
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  /** @deprecated use `owner` instead of `user_id` */
  @Deprecated
  @Column(name = "user_id")
  private String user_id;

  @Column(name = "owner")
  private String owner;

  @Column(name = "date_time")
  private Long date_time; // Comment added/updated time

  @Column(name = "message")
  private String message;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id")
  private CommentEntity commentEntity;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUser_id() {
    return user_id;
  }

  public void setUser_id(String user_id) {
    this.user_id = user_id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Long getDate_time() {
    return date_time;
  }

  public void setDate_time(Long date_time) {
    this.date_time = date_time;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public CommentEntity getCommentEntity() {
    return commentEntity;
  }

  public void setCommentEntity(Object commentEntity) {
    this.commentEntity = (CommentEntity) commentEntity;
    this.entityName = this.commentEntity.getClass().getSimpleName();
  }

  public Comment getProtoObject() {
    return Comment.newBuilder()
        .setId(getId())
        .setUserId(getUser_id())
        .setVertaId(getOwner())
        .setDateTime(getDate_time())
        .setMessage(getMessage())
        .build();
  }
}
