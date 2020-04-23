package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.lineage.LineageEntryContainer;
import ai.verta.modeldb.lineage.VersioningBlobEntryContainer;
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

  @Column(name = "blob_sha")
  private String blobSha;

  @Column(name = "blob_type")
  private String blobType;

  public LineageVersioningBlobEntity() {}

  public LineageVersioningBlobEntity(Long repositoryId, String blobSha, String blobType) {
    this.repositoryId = repositoryId;
    this.blobSha = blobSha;
    this.blobType = blobType;
  }

  public Long getId() {
    return id;
  }

  public LineageEntryContainer getEntry() {
    return new VersioningBlobEntryContainer(repositoryId, blobSha, blobType);
  }
}
