import {
  IDatasetBlob,
  IDatasetBlobDiff,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

import {
  convertServerDatasetPathComponent,
} from '../RepositoryData/Blob/DatasetBlob';
import { IServerBlobDiff, IServerElementDiff, convertServerElementDiffToClient, serverDiffTypeToClient } from './ServerDiff';
import { DataLocation } from 'core/shared/models/Versioning/DataLocation';

const convertServerDatasetDiff = (
  serverDatasetDiff: IServerDatasetDiff,
): IDatasetBlobDiff => {
  const { location } = serverDatasetDiff;
  const serverDatasetBlobType = Object.keys(serverDatasetDiff.dataset)[0] as IDatasetBlobDiff['type'];

  return {
    category: 'dataset',
    diffType: serverDiffTypeToClient(serverDatasetDiff.status),
    location: location as DataLocation,
    data: {
      category: 'dataset',
      type: serverDatasetBlobType,
      components: convertDatasetComponents(
        (serverDatasetDiff.dataset[serverDatasetBlobType]!).components,
        serverDatasetBlobType,
      ),
    } as any,
    type: serverDatasetBlobType,
  };
};

const convertDatasetComponents = (
  serverComponents: IServerComponent[],
  datasetBlobType: IDatasetBlob['type']
) => {
  switch (datasetBlobType) {
    case 'path':
      return (serverComponents as IServerPathDatasetComponentBlobDiff[]).map(path => convertServerElementDiffToClient(convertServerDatasetPathComponent, path));

    case 's3':
      return (serverComponents as IServerS3Component[]).map((d) => ({
        path: convertServerElementDiffToClient(convertServerDatasetPathComponent, d.path),
      }));

    default:
      exhaustiveCheck(datasetBlobType, '');
  }
};

export type IServerDatasetDiff = IServerBlobDiff<{
  dataset: {
    s3?: {
      components: IServerS3Component[];
    };
    path?: {
      components: IServerPathDatasetComponentBlobDiff[];
    };
  }
}>;

type IServerComponent = IServerS3Component | IServerPathDatasetComponentBlobDiff;

type IServerS3Component = { path: IServerPathDatasetComponentBlobDiff };

export type IServerPathDatasetComponentBlobDiff = IServerElementDiff<{
  path?: string; // Full path to the file
  size? : number;
  last_modified_at_source?: number;
  sha256?: string;
  md5?: string;
}>;

export default convertServerDatasetDiff;
