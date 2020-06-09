package ai.verta.modeldb.versioning;

import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class VersioningUtils {
  private static final Logger LOGGER = LogManager.getLogger(VersioningUtils.class);

  private static final String COMMIT_BELONGS_TO_REPO_QUERY =
      "SELECT count(*) FROM CommitEntity c Join c.repository r WHERE c.commit_hash =  :commitHash AND r.id = :repositoryId";
  private static final String GET_PARENT_COMMIT_SHA_PREFIX =
      "SELECT parent_hash FROM commit_parent WHERE child_hash = \'";

  /**
   * Checks the database and returns if a commitHash belongs to a repository
   *
   * @param session
   * @param commitHash : hash of commit
   * @param repositoryId : id of the repository
   * @return
   */
  static boolean commitRepositoryMappingExists(
      Session session, String commitHash, Long repositoryId) {
    Query query = session.createQuery(COMMIT_BELONGS_TO_REPO_QUERY);
    query.setParameter("commitHash", commitHash);
    query.setParameter("repositoryId", repositoryId);
    Long count = (Long) query.getSingleResult();
    return count > 0;
  }

  /**
   * Returns the parents of a commit sorted in descending orderof time
   *
   * @param session
   * @param commitSHA
   * @return
   */
  // TODO: RepoDAO.listBranchCommits should use this
  public static List<CommitEntity> getParentCommits(Session session, String commitSHA) {
    List<String> childCommitSHAs = new LinkedList<String>();
    childCommitSHAs.add(commitSHA);
    List<String> commitSHAs = new LinkedList<>();
    while (!childCommitSHAs.isEmpty()) {
      String childCommit = childCommitSHAs.remove(0);
      commitSHAs.add(childCommit);
      @SuppressWarnings("unchecked")
      Query<String> sqlQuery =
          session.createSQLQuery(GET_PARENT_COMMIT_SHA_PREFIX + childCommit + "\'");
      List<String> parentCommitSHAs = sqlQuery.list();
      childCommitSHAs.addAll(parentCommitSHAs);
    }
    String getChildCommits =
        "FROM "
            + CommitEntity.class.getSimpleName()
            + " c WHERE c.commit_hash IN (:childCommitSHAs)  ORDER BY c.date_created DESC";
    @SuppressWarnings("unchecked")
    Query<CommitEntity> query = session.createQuery(getChildCommits);
    query.setParameterList("childCommitSHAs", commitSHAs);
    return query.list();
  }
  /**
   * Given commit components returns commitSHA
   *
   * @param parentSHAs
   * @param message
   * @param timeCreated
   * @param author
   * @param blobSHA
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static String generateCommitSHA(
      List<String> parentSHAs, String message, long timeCreated, String author, String blobSHA)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    if (!parentSHAs.isEmpty()) {
      parentSHAs = parentSHAs.stream().sorted().collect(Collectors.toList());
      sb.append("parent:");
      parentSHAs.forEach(pSHA -> sb.append(pSHA));
    }
    sb.append(":message:")
        .append(message)
        .append(":date_created:")
        .append(timeCreated)
        .append(":author:")
        .append(author)
        .append(":rootHash:")
        .append(blobSHA);

    return FileHasher.getSha(sb.toString());
  }

  public static String revertCommitMessage(Commit revertCommit) {
    return "Revert \""
        + revertCommit.getMessage()
        + " ("
        + revertCommit.getCommitSha().substring(0, 7)
        + ")\"";
  }

  public static void setPredicatesWithQueryOperator(
      StringBuilder queryStringBuilder, String operatorName, String[] predicateClause) {
    queryStringBuilder.append(String.join(" " + operatorName + " ", predicateClause));
  }

  public static void setQueryParameters(
      StringBuilder queryBuilder, KeyValueQuery keyValueQuery, Map<String, Object> parametersMap) {
    Value value = keyValueQuery.getValue();
    OperatorEnum.Operator operator = keyValueQuery.getOperator();
    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        LOGGER.debug("Called switch case : number_value");
        setValueWithOperatorInQuery(queryBuilder, operator, value.getNumberValue(), parametersMap);
        break;
      case STRING_VALUE:
        LOGGER.debug("Called switch case : string_value");
        if (!value.getStringValue().isEmpty()) {
          LOGGER.debug("Called switch case : string value exist");
          if (keyValueQuery.getKey().equals(ModelDBConstants.REPOSITORY_VISIBILITY)) {
            setValueWithOperatorInQuery(
                queryBuilder,
                operator,
                RepositoryVisibilityEnum.RepositoryVisibility.valueOf(value.getStringValue())
                    .ordinal(),
                parametersMap);
          } else {
            setValueWithOperatorInQuery(
                queryBuilder, operator, value.getStringValue(), parametersMap);
          }
          break;
        } else {
          Status invalidValueTypeError =
              Status.newBuilder()
                  .setCode(com.google.rpc.Code.INVALID_ARGUMENT_VALUE)
                  .setMessage("Predicate does not contain string value in request")
                  .build();
          throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
        }
      case BOOL_VALUE:
        LOGGER.debug("Called switch case : bool_value");
        setValueWithOperatorInQuery(queryBuilder, operator, value.getStringValue(), parametersMap);
        break;
      default:
        Status invalidValueTypeError =
            Status.newBuilder()
                .setCode(Code.UNIMPLEMENTED_VALUE)
                .setMessage(
                    "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE")
                .build();
        throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
    }
  }

  public static void setValueWithOperatorInQuery(
      StringBuilder queryBuilder,
      OperatorEnum.Operator operator,
      Object value,
      Map<String, Object> parametersMap) {
    long timestamp = Math.round(100.0 * Math.random()) + Calendar.getInstance().getTimeInMillis();
    String key;
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.GT_VALUE:
        key = "GT_VALUE_" + timestamp;
        queryBuilder.append(" > :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.GTE_VALUE:
        key = "GTE_VALUE_" + timestamp;
        queryBuilder.append(" >= :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.LT_VALUE:
        key = "LT_VALUE_" + timestamp;
        queryBuilder.append(" < :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.LTE_VALUE:
        key = "LTE_VALUE_" + timestamp;
        queryBuilder.append(" <= :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.NE_VALUE:
        key = "NE_VALUE_" + timestamp;
        queryBuilder.append(" <> :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.CONTAIN_VALUE:
        queryBuilder
            .append(" LIKE ")
            .append(("'%" + Pattern.compile((String) value) + "%'").toLowerCase());
        break;
      case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
        queryBuilder
            .append(" NOT LIKE ")
            .append(("'%" + Pattern.compile((String) value) + "%'").toLowerCase());
        break;
      case OperatorEnum.Operator.IN_VALUE:
        key = "IN_VALUE_" + timestamp;
        queryBuilder.append(" IN (:").append(key).append(")");
        parametersMap.put(key, value);
        break;
      default:
        key = "default_" + timestamp;
        queryBuilder.append(" = :").append(key);
        parametersMap.put(key, value);
    }
    queryBuilder.append(" ");
  }
}
