package ai.verta.modeldb.entities;

import ai.verta.modeldb.EntityComment;
import ai.verta.modeldb.utils.RdbmsUtils;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "comment")
public class CommentEntity {

  public CommentEntity() {}

  public CommentEntity(EntityComment entityComment) {
    setId(entityComment.getId());
    setEntity_id(entityComment.getEntityId());
    setEntity_name(entityComment.getEntityName());
    setComments(
        RdbmsUtils.convertUserCommentsFromUserCommentEntityList(
            this, entityComment.getCommentsList()));
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @Column(name = "entity_id")
  private String entity_id; // entity.id like experimentRun.id, project.id etc.

  @Column(name = "entity_name")
  private String entity_name; // entity name like experimentRun, project etc.

  @OneToMany(
      targetEntity = UserCommentEntity.class,
      mappedBy = "commentEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  private List<UserCommentEntity>
      comments; // list of user comment messages with date, userId and message text.

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEntity_id() {
    return entity_id;
  }

  public void setEntity_id(String entity_id) {
    this.entity_id = entity_id;
  }

  public String getEntity_name() {
    return entity_name;
  }

  public void setEntity_name(String entity_name) {
    this.entity_name = entity_name;
  }

  public List<UserCommentEntity> getComments() {
    return comments;
  }

  public void setComments(List<UserCommentEntity> comments) {
    this.comments = comments;
  }

  public EntityComment getProtoObject() {
    return EntityComment.newBuilder()
        .setId(getId())
        .setEntityId(getEntity_id())
        .setEntityName(getEntity_name())
        .addAllComments(RdbmsUtils.convertUserCommentListFromUserComments(getComments()))
        .build();
  }
}
