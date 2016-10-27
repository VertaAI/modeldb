package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.client.StructFactory;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import modeldb.ProjectEvent;
import modeldb.ProjectEventResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProject {
  @Before
  public void initialize() throws Exception {
    TestBase.clearTables();
  }

  @Test
  public void testCreateProject() throws Exception {
    // Store a project and ensure its ID exceeds 1.
    ProjectEvent pe = StructFactory.makeProjectEvent();
    ProjectEventResponse per = TestBase.server().storeProjectEvent(pe);
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
}
