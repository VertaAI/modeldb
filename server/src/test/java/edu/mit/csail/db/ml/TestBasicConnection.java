package edu.mit.csail.db.ml;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBasicConnection {

  @Before
  public void initialize() throws Exception {
    TestBase.reset();
  }

  @Test
  public void testConnection() throws Exception {
    Assert.assertEquals(200, TestBase.server().testConnection());
  }
}