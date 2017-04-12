package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.ProjectDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import modeldb.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestProject {
  @Before
  public void initialize() throws Exception {
    TestBase.clearTables();
  }

  public void storeProject() throws Exception {
    TestBase.ctx()
      .insertInto(Tables.PROJECT)
      .columns(Tables.PROJECT.ID, Tables.PROJECT.NAME,
        Tables.PROJECT.AUTHOR, Tables.PROJECT.DESCRIPTION, Tables.PROJECT.CREATED)
      .values(1, "name", "author", "description", TestBase.now())
      .execute();
  }

  public void storeProject2() throws Exception {
    TestBase.ctx()
      .insertInto(Tables.PROJECT)
      .columns(Tables.PROJECT.ID, Tables.PROJECT.NAME,
        Tables.PROJECT.AUTHOR, Tables.PROJECT.DESCRIPTION, Tables.PROJECT.CREATED)
      .values(2, "name2", "author", "description", TestBase.now())
      .execute();
  }

  @Test
  public void testCreateProject() throws Exception {
    // Store a project and ensure its ID exceeds 1.
    ProjectEvent pe = StructFactory.makeProjectEvent();
    ProjectEventResponse per = ProjectDao.store(pe, TestBase.ctx());;
    Assert.assertTrue(per.projectId > 0);

    // Make sure we've stored an element.
    Assert.assertEquals(1, TestBase.ctx().selectFrom(Tables.PROJECT).fetch().size());

    // Make sure it has proper values.
    ProjectRecord rec = TestBase.ctx().selectFrom(Tables.PROJECT).fetchOne();
    Assert.assertEquals(pe.getProject().author, rec.getAuthor());
    Assert.assertEquals(pe.getProject().description, rec.getDescription());
    Assert.assertEquals(pe.getProject().name, rec.getName());
    Assert.assertEquals(per.projectId, rec.getId().intValue());
  }

  @Test
  public void testStoreExistingProject() throws Exception {
    storeProject();

    Assert.assertEquals(1, TestBase.ctx().selectFrom(Tables.PROJECT).fetch().size());

    // Try storing another project with the same ID.
    ProjectEvent pe = StructFactory.makeProjectEvent();
    pe.setProject(pe.project.setId(1));
    ProjectDao.store(pe, TestBase.ctx());

    // Ensure that we still only have one project.
    Assert.assertEquals(1, TestBase.ctx().selectFrom(Tables.PROJECT).fetch().size());
  }

  @Test
  public void testDefaultExperiment() throws Exception {
    storeProject();

    // Store two experiments.
    TestBase.ctx().insertInto(Tables.EXPERIMENT).values(1, 1, "default", "description", TestBase.now()).execute();
    TestBase.ctx().insertInto(Tables.EXPERIMENT).values(2, 1, "name", "description", TestBase.now()).execute();

    // Ensure that we correctly identify the default.
    Assert.assertEquals(1, ProjectDao.getDefaultExperiment(1, TestBase.ctx()));
  }

  @Test
  public void testDefaultExperimentWhenEmpty() throws Exception {
    storeProject();

    // If there are no experiments, the default experiment should be -1.
    Assert.assertEquals(-1, ProjectDao.getDefaultExperiment(1, TestBase.ctx()));
  }

  @Test
  public void testReadProject() throws Exception {
    storeProject();

    Project pr = ProjectDao.read(1, TestBase.ctx());
    Assert.assertEquals(1, pr.getId());
    Assert.assertEquals("name", pr.getName());
    Assert.assertEquals("description", pr.getDescription());
    Assert.assertEquals("author", pr.getAuthor());
  }

  @Test
  public void testReadNonExistingProject() throws Exception {
    try {
      ProjectDao.read(1, TestBase.ctx());
      Assert.fail();
    } catch (ResourceNotFoundException ex) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testProjectOverviews() throws Exception {

    // Initially, we should not have any projects in the response.
    List<ProjectOverviewResponse> overview = ProjectDao.getProjectOverviews(TestBase.ctx());
    Assert.assertEquals(0, overview.size());

    // Test when there is a project in the response.
    TestBase.createExperimentRun();
    overview = ProjectDao.getProjectOverviews(TestBase.ctx());
    Assert.assertEquals(1, overview.size());
    Assert.assertEquals("Test project", overview.get(0).getProject().getName());
    Assert.assertEquals(1, overview.get(0).numExperimentRuns);
    Assert.assertEquals(1, overview.get(0).numExperiments);
  }

  @Test
  public void testGetProjectIdsSingle() throws Exception {
    storeProject();

    Map<String, String> kvToMatch = new HashMap<>();
    List<Integer> noMatch = Collections.emptyList();
    List<Integer> oneMatch = Arrays.asList(1);

    // Test when there are no kv pairs
    Assert.assertEquals(oneMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with 1 kv pair that matches
    kvToMatch.put("name", "name");
    Assert.assertEquals(oneMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with 2 kv pairs where 1 doesn't match
    kvToMatch.put("author", "wrong author");
    Assert.assertEquals(noMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
  }

  @Test
  public void testGetProjectIdsMultiple() throws Exception {
    storeProject();
    storeProject2();

    Map<String, String> kvToMatch = new HashMap<>();
    List<Integer> noMatch = Collections.emptyList();
    List<Integer> oneMatch = Arrays.asList(2);
    List<Integer> allMatch = Arrays.asList(1, 2);

    // Test when there are no kv pairs
    Assert.assertEquals(allMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with 1 kv pair that matches all
    kvToMatch.put("author", "author");
    Assert.assertEquals(allMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with kv pair that matches 1
    kvToMatch.put("name", "name2");
    Assert.assertEquals(oneMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with 2 kv pairs where 1 doesn't match
    kvToMatch.put("author", "wrong author");
    Assert.assertEquals(noMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
  }

  @Test
  public void testUpdateProject() throws Exception {
    storeProject();

    Assert.assertTrue(ProjectDao.updateProject(1, "name", "new name", TestBase.ctx()));
    // check that the project is updated
    Map<String, String> kvToMatch = new HashMap<>();
    List<Integer> noMatches = Collections.emptyList();
    List<Integer> oneMatch = Arrays.asList(1);

    // Test with old name
    kvToMatch.put("name", "name");
    Assert.assertEquals(noMatches, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
    // Test with new name
    kvToMatch.put("name", "new name");
    Assert.assertEquals(oneMatch, ProjectDao.getProjectIds(kvToMatch, TestBase.ctx()));
  }
}
