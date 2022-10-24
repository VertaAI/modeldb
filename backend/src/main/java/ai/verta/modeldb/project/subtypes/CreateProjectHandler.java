package ai.verta.modeldb.project.subtypes;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.HandlerUtil;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateProjectHandler extends HandlerUtil {

  private static Logger LOGGER = LogManager.getLogger(CreateProjectHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final Config config;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final CodeVersionHandler codeVersionHandler;

  public CreateProjectHandler(
          FutureExecutor executor,
      FutureJdbi jdbi,
      Config config,
      UAC uac,
      AttributeHandler attributeHandler,
      TagsHandler tagsHandler,
      ArtifactHandler artifactHandler) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.config = config;
    this.uac = uac;

    this.attributeHandler = attributeHandler;
    this.tagsHandler = tagsHandler;
    this.artifactHandler = artifactHandler;
    this.codeVersionHandler = new CodeVersionHandler(executor, jdbi, "project");
  }

  public InternalFuture<Project> convertCreateRequest(final CreateProject request) {
    return InternalFuture.completedInternalFuture(getProjectFromRequest(request));
  }

  /**
   * Method to convert createProject request to Project object. This method generates the project Id
   * using UUID and puts it in Project object.
   *
   * @param request : CreateProject
   * @return Project
   */
  private Project getProjectFromRequest(CreateProject request) {

    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    String projectShortName = ModelDBUtils.convertToProjectShortName(request.getName());

    /*
     * Create Project entity from given CreateProject request. generate UUID and put as id in
     * project for uniqueness. set above created List<KeyValue> attributes in project entity.
     */
    var projectBuilder =
        Project.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setShortName(projectShortName)
            .setDescription(request.getDescription())
            .addAllAttributes(
                request.getAttributesList().stream()
                    .sorted(Comparator.comparing(KeyValue::getKey))
                    .collect(Collectors.toList()))
            .addAllTags(
                TagsHandlerBase.checkEntityTagsLength(request.getTagsList()).stream()
                    .sorted()
                    .collect(Collectors.toList()))
            .setProjectVisibility(request.getProjectVisibility())
            .setVisibility(request.getVisibility())
            .addAllArtifacts(
                request.getArtifactsList().stream()
                    .sorted(Comparator.comparing(Artifact::getKey))
                    .collect(Collectors.toList()))
            .setReadmeText(request.getReadmeText())
            .setCustomPermission(request.getCustomPermission())
            .setVersionNumber(1L);

    if (request.getDateCreated() != 0L) {
      projectBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      projectBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }

    return projectBuilder.build();
  }

  public InternalFuture<Project> insertProject(Project newProject) {
    final var now = Calendar.getInstance().getTimeInMillis();
    Map<String, Object> valueMap = new LinkedHashMap<>();
    valueMap.put("id", newProject.getId());
    valueMap.put("name", newProject.getName());
    valueMap.put("short_name", newProject.getShortName());
    valueMap.put("description", newProject.getDescription());
    valueMap.put("date_created", newProject.getDateCreated());
    valueMap.put("date_updated", newProject.getDateUpdated());
    valueMap.put("owner", newProject.getOwner());
    valueMap.put("version_number", newProject.getVersionNumber());
    valueMap.put("readme_text", newProject.getReadmeText());

    valueMap.put("deleted", false);
    valueMap.put("created", false);
    valueMap.put("visibility_migration", true);

    Supplier<InternalFuture<Project>> insertFutureSupplier =
        () ->
            jdbi.withTransaction(
                handle -> {
                  final var builder = newProject.toBuilder();
                  String queryString = buildInsertQuery(valueMap, "project");

                  LOGGER.trace("insert project query string: " + queryString);
                  var query = handle.createUpdate(queryString);

                  // Inserting fields arguments based on the keys and value of map
                  for (Map.Entry<String, Object> objectEntry : valueMap.entrySet()) {
                    query.bind(objectEntry.getKey(), objectEntry.getValue());
                  }

                  int count = query.execute();
                  LOGGER.trace("Project Inserted : " + (count > 0));

                  if (!builder.getTagsList().isEmpty()) {
                    tagsHandler.addTags(handle, builder.getId(), builder.getTagsList());
                  }
                  if (!builder.getAttributesList().isEmpty()) {
                    attributeHandler.logKeyValues(
                        handle, builder.getId(), builder.getAttributesList());
                  }
                  if (!builder.getArtifactsList().isEmpty()) {
                    var updatedArtifacts =
                        artifactHandler.logArtifacts(
                            handle, builder.getId(), builder.getArtifactsList(), false);
                    builder.clearArtifacts().addAllArtifacts(updatedArtifacts);
                  }
                  if (builder.getCodeVersionSnapshot().hasCodeArchive()
                      || builder.getCodeVersionSnapshot().hasGitSnapshot()) {
                    codeVersionHandler.logCodeVersion(
                        handle, builder.getId(), false, builder.getCodeVersionSnapshot());
                  }
                  return builder.build();
                });
    return InternalFuture.retriableStage(insertFutureSupplier, CommonDBUtil::needToRetry, executor)
        .thenApply(createdProject -> createdProject, executor);
  }
}
