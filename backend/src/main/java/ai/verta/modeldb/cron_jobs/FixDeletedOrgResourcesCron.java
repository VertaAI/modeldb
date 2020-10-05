package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ResourceDAO;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixDeletedOrgResourcesCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(FixDeletedOrgResourcesCron.class);
  private final RoleService roleService;
  private final Integer recordUpdateLimit;
  private final OrganizationResourceDAOs organizationResourceDAOs;

  public FixDeletedOrgResourcesCron(
      RoleService roleService,
      Integer recordUpdateLimit,
      OrganizationResourceDAOs organizationResourceDAOs) {
    this.roleService = roleService;
    this.recordUpdateLimit = recordUpdateLimit;
    this.organizationResourceDAOs = organizationResourceDAOs;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("FixOrgProjectsCron wakeup");

    ModelDBUtils.registeredBackgroundUtilsCount();

    try {
      fixResources(
          organizationResourceDAOs.getProjectDAO(),
          ProjectEntity::getWorkspace,
          ProjectEntity::getId,
          organizationResourceDAOs.getExperimentRunDAO(), "Projects");
      fixResources(
          organizationResourceDAOs.getRepositoryDAO(),
          RepositoryEntity::getWorkspace_id,
          repositoryEntity -> repositoryEntity.getId().toString(),
          organizationResourceDAOs.getExperimentRunDAO(), "Repositories");
    } catch (Exception ex) {
      LOGGER.warn("FixDeletedOrgResourcesCron : Exception: ", ex);
    }
    LOGGER.info("FixDeletedOrgResourcesCron finish tasks and reschedule");
  }

  private <TDAO extends ResourceDAO<T>, T> void fixResources(
      TDAO resourceDAO,
      Function<T, String> getWorkspace,
      Function<T, String> getId,
      ExperimentRunDAO experimentRunDAO, String name)
      throws ModelDBException {
    List<T> allResources = resourceDAO.getOrganizationResources();
    if (allResources == null) {
      allResources = Collections.emptyList();
    }
    int[] counter = new int[] {0};
    List<String> wrongResources =
        allResources.stream()
            .map(
                resource -> {
                  SimpleEntry<String, Set<String>> result =
                      new SimpleEntry<>(getWorkspace.apply(resource), new HashSet<>());
                  result.getValue().add(getId.apply(resource));
                  return result;
                })
            .collect(
                Collectors.toMap(
                    SimpleEntry::getKey,
                    SimpleEntry::getValue,
                    (entry1, entry2) -> {
                      HashSet<String> newEntry = new HashSet<>();
                      newEntry.addAll(entry1);
                      newEntry.addAll(entry2);
                      return newEntry;
                    }))
            .entrySet()
            .stream()
            .filter(
                entry -> {
                  if (counter[0] > recordUpdateLimit) {
                    return false;
                  }
                  try {
                    roleService.getOrgById(entry.getKey());
                  } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode().equals(Code.NOT_FOUND)) {
                      counter[0] += entry.getValue().size();
                      return true;
                    }
                    throw ex;
                  }
                  return false;
                })
            .map(Entry::getValue)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    if (!wrongResources.isEmpty()) {
      resourceDAO.deleteResources(wrongResources, experimentRunDAO);
    }

    LOGGER.debug(
        "{} deleted successfully : Deleted count {}", name, wrongResources.size());
  }
}
