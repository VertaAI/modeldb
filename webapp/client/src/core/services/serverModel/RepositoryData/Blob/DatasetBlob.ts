import {
  IPathDatasetComponentBlob,
  IS3DatasetComponentBlob,
  IPathDatasetBlob,
} from 'core/shared/models/Repository/Blob/DatasetBlob';

export const convertServerDatasetPathComponent = (
  serverDatasetComponent: any
): IPathDatasetComponentBlob => {
  return {
    path: serverDatasetComponent.path,
    size: Number(serverDatasetComponent.size),
    lastModifiedAtSource: new Date(
      Number(serverDatasetComponent.last_modified_at_source)
    ),
    md5: serverDatasetComponent.md5,
    sha256: serverDatasetComponent.sha256,
  };
};

export const convertServerDatasetS3Component = (
  serverDatasetComponent: any
): IS3DatasetComponentBlob => {
  return {
    path: convertServerDatasetPathComponent(serverDatasetComponent.path),
  };
};

export const convertPathDatasetBlob = (serverPath: any): IPathDatasetBlob => {
  return {
    category: 'dataset',
    type: 'path',
    components: serverPath.components.map(convertServerDatasetPathComponent),
  };
};
