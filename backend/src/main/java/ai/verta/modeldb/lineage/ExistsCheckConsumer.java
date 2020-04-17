package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.ModelDBException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.hibernate.Session;

public interface ExistsCheckConsumer {
  void test(Session session, List<LineageEntry> list)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException;
}
