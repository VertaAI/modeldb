package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import modeldb.Project;
import modeldb.ProjectEvent;
import modeldb.ProjectEventResponse;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Date;

public class ProjectDao {
  public static ProjectEventResponse store(ProjectEvent pr, DSLContext ctx) {
    Project p = pr.project;
    ProjectRecord pRec = ctx.newRecord(Tables.PROJECT);
    pRec.setId(p.id < 0 ? null : p.id);
    pRec.setName(p.name);
    pRec.setAuthor(p.author);
    pRec.setDescription(p.description);
    pRec.setCreated(new Timestamp((new Date()).getTime()));
    pRec.store();
    return new ProjectEventResponse(pRec.getId());
  }
}
