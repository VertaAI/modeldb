package ai.verta.modeldb.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_id")
public class LineageIdEntity implements Serializable {

  public LineageIdEntity() {}

  public LineageIdEntity(Long id) {
    this.id = id;
  }

  @Id
  Long id;

  public Long getId() {
    return id;
  }
}
