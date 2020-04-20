package ai.verta.modeldb.entities.lineage;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_elemennt")
public class ElementEntity implements Serializable {

  public ElementEntity() {}

  public ElementEntity(Long id) {
    this.id = id;
  }

  // Autoincrement
  @Id
  Long id;

  public Long getId() {
    return id;
  }
}
