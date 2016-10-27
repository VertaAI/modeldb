package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.ModelDbServer;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static jooq.sqlite.gen.Tables.*;


public class TestBase {
  private static DSLContext context = null;
  private static ModelDbServer server = null;

  private static void createSqliteDb() throws IOException {
    ProcessBuilder pb = new ProcessBuilder("sh", "gen_sqlite.sh");
    pb.directory(new File("codegen/"));
    pb.start();
  }

  public static DSLContext ctx() throws SQLException, IOException, ParseException {
    if (context != null) {
      return context;
    }

    createSqliteDb();
    ModelDbConfig config = ModelDbConfig.parse(new String[] {});
    Connection conn = DriverManager.getConnection(config.jbdcTestUrl, "", "");
    context = DSL.using(conn, SQLDialect.SQLITE);

    return context;
  }

  public static int reset() throws SQLException, IOException, ParseException {
    clearTables();
    return createTestExperimentRun();
  }

  public static Timestamp now() {
    return new Timestamp((new Date()).getTime());
  }

  public static int createTestExperimentRun() throws SQLException, IOException, ParseException {
    Timestamp now = now();
    ProjectRecord projRec = ctx().newRecord(Tables.PROJECT);
    projRec.setId(null);
    projRec.setName("Test project");
    projRec.setAuthor("ModelDB Team");
    projRec.setDescription("Test project");
    projRec.setCreated(now);
    projRec.store();

    ExperimentRecord expRec = ctx().newRecord(Tables.EXPERIMENT);
    expRec.setId(null);
    expRec.setProject(projRec.getId());
    expRec.setName("Test experiment");
    expRec.setDescription("Test experiment");
    expRec.setCreated(now);
    expRec.store();

    ExperimentrunRecord expRunRec = ctx().newRecord(Tables.EXPERIMENTRUN);
    expRunRec.setId(null);
    expRunRec.setExperiment(expRec.getId());
    expRunRec.setDescription("Test experiment run");
    expRunRec.setCreated(now);
    expRunRec.store();

    return expRunRec.getId();
  }

  public static ModelDbServer server() throws SQLException, IOException, ParseException, TException {
    if (server != null) {
      return server;
    }

    server = new ModelDbServer(ctx());

    return server;
  }
  
  public static void clearTables() throws SQLException, IOException, ParseException {
    List<Table> tables = Arrays.asList(
      ANNOTATION,
      ANNOTATIONFRAGMENT,
      CROSSVALIDATIONEVENT,
      CROSSVALIDATIONFOLD,
      DATAFRAME,
      DATAFRAMECOLUMN,
      DATAFRAMESPLIT,
      EVENT,
      EXPERIMENT,
      EXPERIMENTRUN,
      FEATURE,
      FITEVENT,
      GRIDCELLCROSSVALIDATION,
      GRIDSEARCHCROSSVALIDATIONEVENT,
      HYPERPARAMETER,
      LINEARMODEL,
      LINEARMODELTERM,
      METRICEVENT,
      MODELOBJECTIVEHISTORY,
      PIPELINESTAGE,
      PROJECT,
      RANDOMSPLITEVENT,
      TRANSFORMEVENT,
      TRANSFORMER,
      TRANSFORMERSPEC,
      TREELINK,
      TREEMODEL,
      TREEMODELCOMPONENT,
      TREENODE
    );
    for (Table table : tables) {
      ctx().deleteFrom(table).where("1 = 1").execute();
    }
  }
}
