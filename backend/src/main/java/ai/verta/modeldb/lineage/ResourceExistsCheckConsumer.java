package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.ModelDBException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.hibernate.Session;

/**
 * checks that resources exists and throws an exception if not consumer like javalib {@code
 * java.util.function.Consumer} class
 */
public interface ResourceExistsCheckConsumer {
  void accept(Session session, List<LineageEntry> list)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException;
}
