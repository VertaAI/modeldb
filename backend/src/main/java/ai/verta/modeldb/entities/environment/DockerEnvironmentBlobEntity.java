package ai.verta.modeldb.entities.environment;

import ai.verta.modeldb.versioning.DockerEnvironmentBlob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "docker_environment_blob")
public class DockerEnvironmentBlobEntity {

  public DockerEnvironmentBlobEntity() {}

  public DockerEnvironmentBlobEntity(String blobHash, DockerEnvironmentBlob docker) {
    blob_hash = blobHash;
    repository = docker.getRepository();
    tag = docker.getTag();
    sha = docker.getSha();
  }

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(name = "repository")
  private String repository;

  @Column(name = "tag")
  private String tag;

  @Column(name = "sha")
  private String sha;

  public DockerEnvironmentBlob toProto() {
    return DockerEnvironmentBlob.newBuilder()
        .setRepository(repository)
        .setTag(tag)
        .setSha(sha)
        .build();
  }
}
