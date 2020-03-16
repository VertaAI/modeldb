package ai.verta.modeldb.entities;

import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.RdbmsUtils;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "codeversion")
public class CodeVersionEntity {

  public CodeVersionEntity() {}

  public CodeVersionEntity(String fieldType, CodeVersion codeVersion) {

    setDate_logged(codeVersion.getDateLogged());
    if (codeVersion.hasGitSnapshot()) {
      setGit_snapshot(
          RdbmsUtils.generateGitSnapshotEntity(
              ModelDBConstants.GIT_SNAPSHOT, codeVersion.getGitSnapshot()));
    } else if (codeVersion.hasCodeArchive()) {
      setCode_archive(
          RdbmsUtils.generateArtifactEntity(
              this, ModelDBConstants.CODE_ARCHIVE, codeVersion.getCodeArchive()));
    }
    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "date_logged")
  private Long date_logged;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "git_snapshot_id")
  private GitSnapshotEntity git_snapshot;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "code_archive_id")
  private ArtifactEntity code_archive;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getDate_logged() {
    return date_logged;
  }

  public void setDate_logged(Long date_logged) {
    this.date_logged = date_logged;
  }

  public GitSnapshotEntity getGit_snapshot() {
    return git_snapshot;
  }

  public void setGit_snapshot(GitSnapshotEntity git_snapshot) {
    this.git_snapshot = git_snapshot;
  }

  public ArtifactEntity getCode_archive() {
    return code_archive;
  }

  public void setCode_archive(ArtifactEntity code_archive) {
    this.code_archive = code_archive;
  }

  public CodeVersion getProtoObject() {
    CodeVersion.Builder codeVersionBuilder = CodeVersion.newBuilder().setDateLogged(date_logged);
    if (git_snapshot != null) {
      codeVersionBuilder.setGitSnapshot(git_snapshot.getProtoObject());
    } else if (code_archive != null) {
      codeVersionBuilder.setCodeArchive(code_archive.getProtoObject());
    }
    return codeVersionBuilder.build();
  }
}
