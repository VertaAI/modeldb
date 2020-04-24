package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.Location;
import ai.verta.modeldb.lineage.LineageEntryContainer;
import ai.verta.modeldb.lineage.VersioningBlobEntryContainer;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_versioning_blob")
public class LineageVersioningBlobEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "commit_sha")
  private String commitSha;

  @Column(name = "location")
  private String location;

  public LineageVersioningBlobEntity() {}

  public LineageVersioningBlobEntity(
      Long repositoryId, String commitSha, ProtocolStringList location)
      throws InvalidProtocolBufferException {
    this.repositoryId = repositoryId;
    this.commitSha = commitSha;
    this.location = toLocationString(location);
  }

  public static String toLocationString(ProtocolStringList location)
      throws InvalidProtocolBufferException {
    return ModelDBUtils.getStringFromProtoObject(Location.newBuilder().addAllLocation(location));
  }

  public Long getId() {
    return id;
  }

  public LineageEntryContainer getEntry() {
    return new VersioningBlobEntryContainer(repositoryId, commitSha, location);
  }
}
