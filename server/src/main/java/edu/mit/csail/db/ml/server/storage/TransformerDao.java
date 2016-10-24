package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import modeldb.EmptyFieldException;
import modeldb.ResourceNotFoundException;
import modeldb.Transformer;
import org.jooq.DSLContext;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;

import static jooq.sqlite.gen.Tables.TRANSFORMER;

public class TransformerDao {

  public static String path(int id, DSLContext ctx) throws ResourceNotFoundException, EmptyFieldException {
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(id)).fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(
        String.format("Could not find path to model file of Transformer with id %d", id)
      );
    }
    if (rec.getFilepath() == null || rec.getFilepath().equals(""))  {
      throw new EmptyFieldException(
        String.format("The Transformer with id %d does not have a model file", id)
      );
    }
    return rec.getFilepath();
  }

  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(id)).fetchOne() != null;
  }

  private static String generateFilepath() {
    String uuid = UUID.randomUUID().toString();
    return Paths.get(ModelDbConfig.getInstance().fsPrefix, uuid).toString();
  }

  public static TransformerRecord store(Transformer t, int experimentId, DSLContext ctx, boolean generateFilepath) {
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(t.id)).fetchOne();
    if (rec != null) {
      if (generateFilepath && rec.getFilepath().length() == 0) {
        rec.setFilepath(generateFilepath());
        rec.store();
      }
      return rec;
    }

    final TransformerRecord tRec = ctx.newRecord(TRANSFORMER);
    tRec.setId(null);
    tRec.setTransformertype(t.transformerType);
    tRec.setWeights(t.weights.stream().map(String::valueOf).collect(Collectors.joining(",")));
    tRec.setTag(t.tag);
    tRec.setExperimentrun(experimentId);


    // Generate a UUID and filepath.
    String filePath = generateFilepath ? generateFilepath() : "";

    tRec.setFilepath(filePath);

    tRec.store();
    return tRec;
  }

  public static TransformerRecord store(Transformer t, int experimentId, DSLContext ctx) {
    return store(t, experimentId, ctx, false);
  }
}
