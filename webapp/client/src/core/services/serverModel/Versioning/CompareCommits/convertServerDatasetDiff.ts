import {
  IDatasetBlob,
  IDatasetBlobDiff,
  IDatasetBlobDiffData,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

import {
  convertServerDatasetPathComponent,
  convertServerDatasetS3Component,
} from '../RepositoryData/Blob/DatasetBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerElementDiffToClient,
  convertServerBlobDiffToClient,
} from './ServerDiff';

const convertServerDatasetDiff = (
  serverDatasetDiff: IServerDatasetDiff
): IDatasetBlobDiff => {
  const serverDatasetBlobType = Object.keys(
    serverDatasetDiff.dataset
  )[0] as IDatasetBlobDiff['type'];

  return convertServerBlobDiffToClient<IServerDatasetDiff, IDatasetBlobDiff>(
    {
      category: 'dataset',
      type: serverDatasetBlobType,
      convertData: () => {
        return {
          category: 'dataset',
          type: serverDatasetBlobType,
          components: convertDatasetComponents(
            serverDatasetDiff.dataset[serverDatasetBlobType]!.components,
            serverDatasetBlobType
          ),
        } as IDatasetBlobDiffData;
      },
    },
    serverDatasetDiff
  );
};

const convertDatasetComponents = (
  serverComponents: IServerComponent[],
  datasetBlobType: IDatasetBlob['type']
) => {
  switch (datasetBlobType) {
    case 'path':
      return (serverComponents as IServerPathDatasetComponentBlobDiff[]).map(
        path =>
          convertServerElementDiffToClient(
            convertServerDatasetPathComponent,
            path
          )
      );

    case 's3':
      return (serverComponents as IServerS3DatasetComponentBlobDiff[]).map(
        component =>
          convertServerElementDiffToClient(
            convertServerDatasetS3Component,
            component
          )
      );

    default:
      exhaustiveCheck(datasetBlobType, '');
  }
};

export type IServerDatasetDiff = IServerBlobDiff<{
  dataset: {
    s3?: {
      components: IServerS3DatasetComponentBlobDiff[];
    };
    path?: {
      components: IServerPathDatasetComponentBlobDiff[];
    };
  };
}>;

export interface IServerPathDatasetComponent {
  path?: string; // Full path to the file
  size?: number;
  last_modified_at_source?: number;
  sha256?: string;
  md5?: string;
}

interface IServerS3Component {
  path: IServerPathDatasetComponent;
  s3_version_id: string;
}

type IServerComponent =
  | IServerS3DatasetComponentBlobDiff
  | IServerPathDatasetComponentBlobDiff;

export type IServerS3DatasetComponentBlobDiff = IServerElementDiff<
  IServerS3Component
>;

export type IServerPathDatasetComponentBlobDiff = IServerElementDiff<
  IServerPathDatasetComponent
>;

export default convertServerDatasetDiff;
