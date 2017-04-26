package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.ModelDbServer;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TException;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;
import edu.mit.csail.db.ml.server.storage.metadata.MongoMetadataDb;

import static jooq.sqlite.gen.Tables.*;


public class TestBase {
  public static class ProjExpRunTriple {
    public final int projId;
    public final int expId;
    public final int expRunId;
    public ProjExpRunTriple(int projId, int expId, int expRunId) {
      this.projId = projId;
      this.expId = expId;
      this.expRunId = expRunId;
    }
  }

  private static DSLContext context = null;
  private static ModelDbServer server = null;
  private static ModelDbConfig config = null;
  private static MetadataDb metadataDb = null;

  private static void createSqliteDb() throws IOException {
    ProcessBuilder pb = new ProcessBuilder("sh", "gen_sqlite_test.sh");
    pb.directory(new File("codegen/"));
    pb.start();
  }

  public static ModelDbConfig getConfig() {
    return config;
  }

  public static MetadataDb getMetadataDb() throws ParseException {
    if (metadataDb != null) {
      return metadataDb;
    }

    config = ModelDbConfig.parse(new String[] {});
    metadataDb = new MongoMetadataDb(config.metadataDbHost, 
      config.metadataDbPort, config.metadataDbTestDbName);
    metadataDb.open();
    return metadataDb;
  }

  public static DSLContext ctx() throws SQLException, IOException, ParseException {
    if (context != null) {
      return context;
    }

    createSqliteDb();
    config = ModelDbConfig.parse(new String[] {});
    Connection conn = DriverManager.getConnection(config.jbdcTestUrl, "", "");
    context = DSL.using(conn, SQLDialect.SQLITE);

    return context;
  }

  public static ProjExpRunTriple reset() throws Exception {
    clearTables();
    // clear the metadata test database
    resetMetadataDb();
    // this is required. other tests use the generated experiment run
    return createExperimentRun();
  }

  public static void resetMetadataDb() {
    if (metadataDb != null) {
      metadataDb.reset();
    }
  }

  public static Timestamp now() {
    return new Timestamp((new Date()).getTime());
  }

  public static ProjExpRunTriple createProject() throws Exception {
    ProjectRecord projRec = ctx().newRecord(Tables.PROJECT);
    projRec.setId(null);
    projRec.setName("Test project");
    projRec.setAuthor("ModelDB Team");
    projRec.setDescription("Test project");
    projRec.setCreated(now());
    projRec.store();
    return new ProjExpRunTriple(projRec.getId(), -1, -1);
  }

  public static ProjExpRunTriple createExperiment(int projId) throws Exception {
    ExperimentRecord expRec = ctx().newRecord(Tables.EXPERIMENT);
    expRec.setId(null);
    expRec.setProject(projId);
    expRec.setName("Test experiment");
    expRec.setDescription("Test experiment description");
    expRec.setCreated(now());
    expRec.store();
    return new ProjExpRunTriple(projId, expRec.getId(), -1);
  }

  public static ProjExpRunTriple createExperiment() throws Exception {
    return createExperiment(createProject().projId);
  }

  public static ProjExpRunTriple createExperimentRunHelper(
    boolean hasSha) throws Exception {
    ProjExpRunTriple triple = createExperiment();

    ExperimentrunRecord expRunRec = ctx().newRecord(Tables.EXPERIMENTRUN);
    expRunRec.setId(null);
    expRunRec.setExperiment(triple.expId);
    expRunRec.setDescription("Test experiment run");
    expRunRec.setCreated(now());
    if (hasSha) {
      expRunRec.setSha("A1B2C3D4E5");
    }
    expRunRec.store();

    return new ProjExpRunTriple(triple.projId, triple.expId, expRunRec.getId());
  }

  public static ProjExpRunTriple createExperimentRun() throws Exception {
    return createExperimentRunHelper(false);
  }

  public static ProjExpRunTriple createExperimentRunWithSha() throws Exception {
    return createExperimentRunHelper(true);
  }

  public static int createDataFrame(int expRunId, int numRows) throws Exception {
    DataframeRecord rec = ctx().newRecord(Tables.DATAFRAME);
    rec.setId(null);
    rec.setTag("tag");
    rec.setNumrows(numRows);
    rec.setExperimentrun(expRunId);
    rec.store();

    DataframecolumnRecord col1 = ctx().newRecord(Tables.DATAFRAMECOLUMN);
    col1.setId(null);
    col1.setDfid(rec.getId());
    col1.setName("first");
    col1.setType("string");
    col1.store();
    col1.getId();

    DataframecolumnRecord col2 = ctx().newRecord(Tables.DATAFRAMECOLUMN);
    col2.setId(null);
    col2.setDfid(rec.getId());
    col2.setName("second");
    col2.setType("int");
    col2.store();
    col2.getId();
    return rec.getId();
  }

  public static int createTransformer(int expRunId, String transformerType, String filepath) throws Exception {
    TransformerRecord rec = ctx().newRecord(Tables.TRANSFORMER);
    rec.setId(null);
    rec.setTransformertype(transformerType);
    rec.setTag("");
    rec.setExperimentrun(expRunId);
    rec.setFilepath(filepath);
    rec.store();
    return rec.getId();
  }

  public static int createTransformerSpec(int expRunId, String transformerType) throws Exception {
    TransformerspecRecord rec = ctx().newRecord(Tables.TRANSFORMERSPEC);
    rec.setId(null);
    rec.setTransformertype(transformerType);
    rec.setTag("");
    rec.setExperimentrun(expRunId);
    rec.store();

    HyperparameterRecord hp1 = ctx().newRecord(Tables.HYPERPARAMETER);
    hp1.setId(null);
    hp1.setSpec(rec.getId());
    hp1.setParamname("hp1");
    hp1.setParamtype("string");
    hp1.setParamvalue("val1");
    hp1.setParamminvalue(1.0f);
    hp1.setParammaxvalue(2.0f);
    hp1.setExperimentrun(expRunId);
    hp1.store();
    hp1.getId();

    HyperparameterRecord hp2 = ctx().newRecord(Tables.HYPERPARAMETER);
    hp2.setId(null);
    hp2.setSpec(rec.getId());
    hp2.setParamname("hp2");
    hp2.setParamtype("int");
    hp2.setParamvalue("val2");
    hp2.setParamminvalue(3.0f);
    hp2.setParammaxvalue(4.0f);
    hp2.setExperimentrun(expRunId);
    hp2.store();
    hp2.getId();

    return rec.getId();
  }

  public static int createEvent(int evId, int expRunId, String eventType) throws Exception {
    EventRecord rec = ctx().newRecord(Tables.EVENT);
    rec.setId(null);
    rec.setEventtype(eventType);
    rec.setEventid(evId);
    rec.setExperimentrun(expRunId);
    rec.store();
    return rec.getId();
  }

  public static int createFitEvent(int expRunId, int dfId, int specId, int tId) throws Exception {
    FiteventRecord rec = ctx().newRecord(Tables.FITEVENT);
    rec.setId(null);
    rec.setTransformerspec(specId);
    rec.setTransformer(tId);
    rec.setDf(dfId);
    rec.setPredictioncolumns("predCol1,predCol2");
    rec.setLabelcolumns("labCol1,labCol2");
    rec.setExperimentrun(expRunId);
    rec.setProblemtype("regression");
    rec.store();

    // Store Feature columns.
    List<String> features = Arrays.asList("featCol1", "featCol2", "featCol3");
    for (int i = 0; i < features.size(); i++) {
      FeatureRecord ft = ctx().newRecord(Tables.FEATURE);
      ft.setId(null);
      ft.setName(features.get(i));
      ft.setFeatureindex(i);
      ft.setImportance(0.0);
      ft.setTransformer(tId);
      ft.store();
      ft.getId();
    }

    // Store the corresponding event.
    createEvent(rec.getId(), expRunId, "fit");

    return rec.getId();
  }

  public static int createTransformEvent(int expRunId,
                                         int tId,
                                         int oldDfId,
                                         int newDfId,
                                         String inCols,
                                         String outCols) throws Exception {
    TransformeventRecord rec = ctx().newRecord(Tables.TRANSFORMEVENT);
    rec.setId(null);
    rec.setOlddf(oldDfId);
    rec.setNewdf(newDfId);
    rec.setTransformer(tId);
    rec.setInputcolumns(inCols);
    rec.setOutputcolumns(outCols);
    rec.setExperimentrun(expRunId);
    rec.store();

    createEvent(rec.getId(), expRunId, "transform");

    return rec.getId();
  }

  // This method is included here just in case, but try to avoid creating a ModelDbServer in the unit tests.
  // Instead test the *Dao classes (e.g. ProjectDao, FitEventDao).
  public static ModelDbServer server() throws SQLException, IOException, ParseException, TException {
    if (server != null) {
      return server;
    }

    server = new ModelDbServer(ctx());

    return server;
  }

  public static int tableSize(Table t) throws Exception {
    return ctx().selectFrom(t).fetch().size();
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
    List<Query> queries = new ArrayList<>();
    for (Table table : tables) {
      queries.add(ctx().deleteFrom(table).where("1 = 1"));
    }
    ctx().batch(queries).execute();
  }
}
