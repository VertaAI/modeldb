package ai.verta.modeldb.entities.lineage;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_element")
public class LineageElementEntity {

  public LineageElementEntity() {}

  public LineageElementEntity(Long id) {
    this.id = id;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  public Long getId() {
    return id;
  }
}
