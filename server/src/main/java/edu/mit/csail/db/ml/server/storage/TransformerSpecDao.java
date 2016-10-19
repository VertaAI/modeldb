package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.HyperparameterRecord;
import jooq.sqlite.gen.tables.records.TransformerspecRecord;
import modeldb.TransformerSpec;
import org.jooq.DSLContext;

public class TransformerSpecDao {
  public static TransformerspecRecord store(TransformerSpec s, int projId, int experimentId, DSLContext ctx) {
    TransformerspecRecord rec = ctx
      .selectFrom(Tables.TRANSFORMERSPEC)
      .where(Tables.TRANSFORMERSPEC.ID.eq(s.id))
      .fetchOne();

    if (rec != null) {
      return rec;
    }

    TransformerspecRecord sRec = ctx.newRecord(Tables.TRANSFORMERSPEC);
    sRec.setId(null);
    sRec.setProject(projId);
    sRec.setExperimentrun(experimentId);
    sRec.setTag(s.tag);
    sRec.setTransformertype(s.transformerType);
    sRec.store();

    s.hyperparameters.forEach(hp -> {
      HyperparameterRecord hpRec = ctx.newRecord(Tables.HYPERPARAMETER);
      hpRec.setId(null);
      hpRec.setSpec(sRec.getId());
      hpRec.setParamname(hp.name);
      hpRec.setParamtype(hp.type);
      hpRec.setParamvalue(hp.value);
      hpRec.setParamminvalue(Double.valueOf(hp.min).floatValue());
      hpRec.setParammaxvalue(Double.valueOf(hp.max).floatValue());
      hpRec.setProject(projId);
      hpRec.setExperimentrun(experimentId);
      hpRec.store();
      hpRec.getId();
    });

    sRec.getId();
    return sRec;
  }
}
