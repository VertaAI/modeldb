package ai.verta.modeldb.entities;

import ai.verta.modeldb.QueryParameter;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.Value.Builder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "query_parameter")
public class QueryParameterEntity {

  public QueryParameterEntity() {}

  public QueryParameterEntity(Object entity, String fieldType, QueryParameter queryParameter)
      throws InvalidProtocolBufferException {
    setParameter_name(queryParameter.getParameterName());
    setValue(ModelDBUtils.getStringFromProtoObject(queryParameter.getValue()));
    setParameter_type(queryParameter.getParameterTypeValue());

    if (entity instanceof QueryDatasetVersionInfoEntity) {
      setQueryDatasetVersionInfoEntity(entity);
    }

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "parameter_name", columnDefinition = "TEXT")
  private String parameter_name;

  @Column(name = "parameter_value", columnDefinition = "TEXT")
  private String value;

  @Column(name = "parameter_type")
  private Integer parameter_type;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "query_dataset_version_info_id")
  private QueryDatasetVersionInfoEntity queryDatasetVersionInfoEntity;

  @Column(name = "entity_name", length = 50)
  private String entity_name;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getParameter_name() {
    return parameter_name;
  }

  public void setParameter_name(String parameter_name) {
    this.parameter_name = parameter_name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getParameter_type() {
    return parameter_type;
  }

  public void setParameter_type(Integer parameter_type) {
    this.parameter_type = parameter_type;
  }

  public QueryDatasetVersionInfoEntity getQueryDatasetVersionInfoEntity() {
    return queryDatasetVersionInfoEntity;
  }

  public void setQueryDatasetVersionInfoEntity(Object queryDatasetVersionInfoEntity) {
    this.queryDatasetVersionInfoEntity =
        (QueryDatasetVersionInfoEntity) queryDatasetVersionInfoEntity;
    this.entity_name = this.queryDatasetVersionInfoEntity.getClass().getSimpleName();
  }

  public String getField_type() {
    return field_type;
  }

  public QueryParameter getProtoObject() throws InvalidProtocolBufferException {
    Builder valueBuilder = Value.newBuilder();
    valueBuilder = (Builder) ModelDBUtils.getProtoObjectFromString(getValue(), valueBuilder);
    return QueryParameter.newBuilder()
        .setParameterName(getParameter_name())
        .setValue(valueBuilder.build())
        .setParameterTypeValue(getParameter_type())
        .build();
  }
}
