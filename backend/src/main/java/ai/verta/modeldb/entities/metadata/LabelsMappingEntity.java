package ai.verta.modeldb.entities.metadata;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.VersioningCompositeIdentifier;
import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "labels_mapping")
public class LabelsMappingEntity {
  public LabelsMappingEntity() {}

  public LabelsMappingEntity(LabelMappingId id) {
    this.id = id;
  }

  @EmbeddedId private LabelMappingId id;

  public static LabelMappingId createId(IdentificationType id, String label)
      throws ModelDBException {
    if (id.getIdCase().equals(IdentificationType.IdCase.INT_ID)) {
      return new LabelMappingId(String.valueOf(id.getIntId()), id.getIdTypeValue(), label);
    } else if (id.getIdCase().equals(IdentificationType.IdCase.STRING_ID)) {
      return new LabelMappingId(id.getStringId(), id.getIdTypeValue(), label);
    } else if (id.getIdCase().equals(IdentificationType.IdCase.COMPOSITE_ID)) {
      String compositeId = getVersioningCompositeIdString(id.getCompositeId(), id.getIdType());
      return new LabelMappingId(compositeId, id.getIdTypeValue(), label);
    } else {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
    }
  }

  public LabelMappingId getId() {
    return id;
  }

  public String getValue() {
    return id.getLabel();
  }

  @Embeddable
  public static class LabelMappingId implements Serializable {

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "entity_hash", nullable = false, columnDefinition = "varchar")
    private String entity_hash;

    @Column(name = "entity_type")
    private Integer entity_type;

    public LabelMappingId(String entityHash, Integer entityType, String label) {
      this.entity_hash = entityHash;
      this.entity_type = entityType;
      this.label = label;
    }

    private LabelMappingId() {}

    public String getEntity_hash() {
      return entity_hash;
    }

    public Integer getEntity_type() {
      return entity_type;
    }

    public String getLabel() {
      return label;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof LabelMappingId)) return false;
      LabelMappingId that = (LabelMappingId) o;
      return Objects.equals(getEntity_hash(), that.getEntity_hash())
          && Objects.equals(getEntity_type(), that.getEntity_type())
          && Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getEntity_hash(), getEntity_type(), getLabel());
    }
  }

  public static String getVersioningCompositeIdString(
      VersioningCompositeIdentifier identifier, IDTypeEnum.IDType idType) throws ModelDBException {
    if (idType.equals(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)) {
      return identifier.getRepoId()
          + "::"
          + identifier.getCommitHash()
          + "::"
          + ModelDBUtils.getLocationWithSlashOperator(identifier.getLocationList());
    } else {
      throw new ModelDBException(
          "Invalid argument found in VersioningCompositeIdentifier", Status.Code.INVALID_ARGUMENT);
    }
  }

  public static VersioningCompositeIdentifier getVersioningCompositeId(String identifierIdStr) {
    String[] identifierArr = identifierIdStr.split("::");
    if (identifierArr.length == 3) {
      return VersioningCompositeIdentifier.newBuilder()
          .setRepoId(Long.parseLong(identifierArr[0]))
          .setCommitHash(identifierArr[1])
          .addAllLocation(ModelDBUtils.getLocationWithSplitSlashOperator(identifierArr[2]))
          .build();
    } else {
      return VersioningCompositeIdentifier.newBuilder()
          .setRepoId(Long.parseLong(identifierArr[0]))
          .setCommitHash(identifierArr[1])
          .build();
    }
  }
}
