package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.EventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestEvent {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset().expRunId;
  }

  @Test
  public void testStore() throws Exception {
    // Try storing an event.
    EventDao.store(2, "testevent", expRunId, TestBase.ctx());


    // Verify that the proper value is stored.
    Assert.assertEquals(1, TestBase.tableSize(Tables.EVENT));
    EventRecord rec = TestBase.ctx().selectFrom(Tables.EVENT).fetchOne();
    Assert.assertEquals(2, rec.getEventid().intValue());
    Assert.assertEquals("testevent", rec.getEventtype());
    Assert.assertEquals(expRunId, rec.getExperimentrun().intValue());
  }
}
