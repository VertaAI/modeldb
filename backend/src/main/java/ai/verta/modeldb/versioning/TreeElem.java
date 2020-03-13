package ai.verta.modeldb.versioning;

import static ai.verta.modeldb.versioning.BlobDAORdbImpl.TREE;

import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public class TreeElem {
  private String path;
  private String blobHash = null;
  private String type = null;
  private Map<String, TreeElem> children = new HashMap<>();

  TreeElem() {}

  public TreeElem push(List<String> pathList, String blobHash, String type) {
    path = pathList.get(0);
    if (pathList.size() > 1) {
      children.putIfAbsent(pathList.get(1), new TreeElem());
      if (this.type == null) this.type = TREE;
      return children
          .get(pathList.get(1))
          .push(pathList.subList(1, pathList.size()), blobHash, type);
    } else {
      this.blobHash = blobHash;
      this.type = type;
      return this;
    }
  }

  String getPath() {
    return path != null ? path : "";
  }

  String getBlobHash() {
    return blobHash;
  }

  String getType() {
    return type;
  }

  InternalFolderElement saveFolders(Session session, FileHasher fileHasher)
      throws NoSuchAlgorithmException {
    if (children.isEmpty()) {
      return InternalFolderElement.newBuilder()
          .setElementName(getPath())
          .setElementSha(getBlobHash())
          .build();
    } else {
      InternalFolder.Builder internalFolder = InternalFolder.newBuilder();
      List<InternalFolderElement> elems = new LinkedList<>();
      for (TreeElem elem : children.values()) {
        InternalFolderElement build = elem.saveFolders(session, fileHasher);
        elems.add(build);
        if (elem.getType().equals(TREE)) {
          internalFolder.addSubFolders(build);
        } else {
          internalFolder.addBlobs(build);
        }
      }
      final InternalFolderElement treeBuild =
          InternalFolderElement.newBuilder()
              .setElementName(getPath())
              .setElementSha(fileHasher.getSha(internalFolder.build()))
              .build();
      Iterator<TreeElem> iter = children.values().iterator();
      for (InternalFolderElement elem : elems) {
        final TreeElem next = iter.next();
        InternalFolderElementEntity internalFolderElementEntity;
        if (next.getType().equals(TREE)) {
          internalFolderElementEntity =
              new InternalFolderElementEntity(elem, treeBuild.getElementSha(), next.getType());
        } else {
          internalFolderElementEntity =
              new InternalFolderElementEntity(
                  treeBuild.getElementSha(), next.getBlobHash(), next.getType(), next.getPath());
        }
        session.saveOrUpdate(internalFolderElementEntity);
      }
      return treeBuild;
    }
  }
}
