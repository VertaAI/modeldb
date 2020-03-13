package ai.verta.modeldb.artifactStore.storageservice;

public interface ArtifactStoreService {

  String generatePresignedUrl(String s3Key, String method);
}
