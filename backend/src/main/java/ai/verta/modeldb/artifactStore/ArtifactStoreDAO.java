package ai.verta.modeldb.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.ModelDBException;

public interface ArtifactStoreDAO {
  GetUrlForArtifact.Response getUrlForArtifact(String path, String method) throws ModelDBException;
}
