package ai.verta.modeldb.entities;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.QueryDatasetVersionInfo;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "query_dataset_version_info")
public class QueryDatasetVersionInfoEntity {

  public QueryDatasetVersionInfoEntity() {}

  public QueryDatasetVersionInfoEntity(
      String fieldType, QueryDatasetVersionInfo queryDatasetVersionInfo)
      throws InvalidProtocolBufferException {

    setQuery(queryDatasetVersionInfo.getQuery());
    setQuery_template(queryDatasetVersionInfo.getQueryTemplate());
    setQuery_parameters(
        RdbmsUtils.convertQueryParametersFromQueryParameterEntityList(
            this,
            ModelDBConstants.QUERY_DATSET_VERSION_INFO,
            queryDatasetVersionInfo.getQueryParametersList()));
    setData_source_uri(queryDatasetVersionInfo.getDataSourceUri());
    setExecution_timestamp(queryDatasetVersionInfo.getExecutionTimestamp());
    setNum_records(queryDatasetVersionInfo.getNumRecords());

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "query", columnDefinition = "TEXT")
  private String query;

  @Column(name = "query_template", columnDefinition = "TEXT")
  private String query_template;

  @OneToMany(
      targetEntity = QueryParameterEntity.class,
      mappedBy = "queryDatasetVersionInfoEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<QueryParameterEntity> query_parameters;

  @Column(name = "data_source_uri")
  private String data_source_uri;

  @Column(name = "execution_timestamp")
  private Long execution_timestamp;

  @Column(name = "num_records")
  private Long num_records;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery_template() {
    return query_template;
  }

  public void setQuery_template(String query_template) {
    this.query_template = query_template;
  }

  public List<QueryParameterEntity> getQuery_parameters() {
    return query_parameters;
  }

  public void setQuery_parameters(List<QueryParameterEntity> query_parameters) {
    this.query_parameters = query_parameters;
  }

  public String getData_source_uri() {
    return data_source_uri;
  }

  public void setData_source_uri(String data_source_uri) {
    this.data_source_uri = data_source_uri;
  }

  public Long getExecution_timestamp() {
    return execution_timestamp;
  }

  public void setExecution_timestamp(Long execution_timestamp) {
    this.execution_timestamp = execution_timestamp;
  }

  public Long getNum_records() {
    return num_records;
  }

  public void setNum_records(Long num_records) {
    this.num_records = num_records;
  }

  public QueryDatasetVersionInfo getProtoObject() throws InvalidProtocolBufferException {
    return QueryDatasetVersionInfo.newBuilder()
        .setQuery(getQuery())
        .setQueryTemplate(getQuery_template())
        .addAllQueryParameters(
            RdbmsUtils.convertQueryParameterEntityListFromQueryParameters(getQuery_parameters()))
        .setDataSourceUri(getData_source_uri())
        .setExecutionTimestamp(getExecution_timestamp())
        .setNumRecords(getNum_records())
        .build();
  }
}
