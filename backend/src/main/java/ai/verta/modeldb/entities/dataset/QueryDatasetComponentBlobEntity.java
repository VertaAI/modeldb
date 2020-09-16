package ai.verta.modeldb.entities.dataset;

import ai.verta.modeldb.versioning.QueryDatasetComponentBlob;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "query_dataset_component_blob")
public class QueryDatasetComponentBlobEntity {

  public QueryDatasetComponentBlobEntity() {}

  public QueryDatasetComponentBlobEntity(
      String blobHash,
      String blobHashDataset,
      QueryDatasetComponentBlob queryDatasetComponentBlob) {
    this.id = new QueryDatasetComponentBlobId(blobHash, blobHashDataset);
    this.query = queryDatasetComponentBlob.getQuery();
    this.data_source_uri = queryDatasetComponentBlob.getDataSourceUri();
    this.execution_timestamp = queryDatasetComponentBlob.getExecutionTimestamp();
    this.num_records = queryDatasetComponentBlob.getNumRecords();
  }

  @EmbeddedId private QueryDatasetComponentBlobId id;

  @Column(name = "query", columnDefinition = "TEXT")
  private String query;

  @Column(name = "data_source_uri", columnDefinition = "TEXT")
  private String data_source_uri;

  @Column(name = "execution_timestamp")
  private Long execution_timestamp;

  @Column(name = "num_records")
  private Long num_records;

  public QueryDatasetComponentBlob toProto() {
    return QueryDatasetComponentBlob.newBuilder()
        .setQuery(this.query)
        .setDataSourceUri(this.data_source_uri)
        .setExecutionTimestamp(this.execution_timestamp)
        .setNumRecords(this.num_records)
        .build();
  }
}

@Embeddable
class QueryDatasetComponentBlobId implements Serializable {

  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(
      name = "query_dataset_blob_id",
      nullable = false,
      columnDefinition = "varchar",
      length = 64)
  private String query_dataset_blob_id;

  public QueryDatasetComponentBlobId(String blobHash, String datasetBlobHash) {
    this.blob_hash = blobHash;
    query_dataset_blob_id = datasetBlobHash;
  }

  private QueryDatasetComponentBlobId() {}

  public String getBlob_hash() {
    return blob_hash;
  }

  public String getQuery_dataset_blob_id() {
    return query_dataset_blob_id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof QueryDatasetComponentBlobId)) return false;
    QueryDatasetComponentBlobId that = (QueryDatasetComponentBlobId) o;
    return Objects.equals(getBlob_hash(), that.getBlob_hash())
        && Objects.equals(getQuery_dataset_blob_id(), that.getQuery_dataset_blob_id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBlob_hash(), getQuery_dataset_blob_id());
  }
}
