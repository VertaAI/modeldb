package ai.verta.modeldb.entities.environment;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "environment_command_line")
public class EnvironmentCommandLineEntity implements Serializable {
  public EnvironmentCommandLineEntity() {}

  @Id
  @ManyToOne(targetEntity = EnvironmentBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "environment_blob_hash")
  private EnvironmentBlobEntity environmentBlobEntity;

  @Id
  @Column(name = "command_seq_number")
  private Integer command_seq_number;

  @Column(name = "command", columnDefinition = "TEXT")
  private String command;

  public EnvironmentCommandLineEntity(int commandSeqNumber, String command) {
    command_seq_number = commandSeqNumber;
    this.command = command;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnvironmentCommandLineEntity that = (EnvironmentCommandLineEntity) o;
    return Objects.equals(environmentBlobEntity, that.environmentBlobEntity)
        && Objects.equals(command_seq_number, that.command_seq_number)
        && Objects.equals(command, that.command);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentBlobEntity, command_seq_number, command);
  }

  public void setEnvironmentBlobEntity(EnvironmentBlobEntity environmentBlobEntity) {
    this.environmentBlobEntity = environmentBlobEntity;
  }

  public String getCommand() {
    return command;
  }
}
