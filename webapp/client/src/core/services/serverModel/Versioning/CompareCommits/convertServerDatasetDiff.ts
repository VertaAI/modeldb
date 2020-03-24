import {
  datasetBlobTypes,
  IDatasetBlob,
  IDatasetBlobDiff,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';
import {
  DiffType,
  IUnknownBlobDiff,
} from 'core/shared/models/Versioning/Blob/Diff';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

import {
  convertServerDatasetPathComponent,
  convertServerDatasetS3Component,
} from '../RepositoryData/Blob/DatasetBlob';

const convertServerDatasetDiff = (
  serverDatasetDiff: any,
  diffType: DiffType
): IDatasetBlobDiff | IUnknownBlobDiff => {
  const category = 'dataset';
  const { location } = serverDatasetDiff;
  const serverDatasetBlobType = Object.keys(serverDatasetDiff[category])[0];
  if (!(serverDatasetBlobType in datasetBlobTypes)) {
    return {
      diffType: 'updated',
      location,
      category: 'unknown',
      type: 'unknown',
      blobA: {
        category: 'unknown',
        data: serverDatasetDiff[category],
      },
      blobB: {
        category: 'unknown',
        data: serverDatasetDiff[category],
      },
    };
  }

  switch (diffType) {
    case 'added':
    case 'deleted': {
      return {
        location,
        diffType,
        category,
        type: serverDatasetBlobType as IDatasetBlob['type'],
        blob: {
          category,
          type: serverDatasetBlobType as IDatasetBlob['type'],
          components: convertDatasetComponents(
            serverDatasetDiff[category][serverDatasetBlobType].A ||
              serverDatasetDiff[category][serverDatasetBlobType].B,
            serverDatasetBlobType as IDatasetBlob['type']
          ),
        },
      } as IDatasetBlobDiff;
    }

    case 'updated': {
      return {
        location,
        diffType,
        category,
        type: serverDatasetBlobType as IDatasetBlob['type'],
        blobA: serverDatasetDiff[category][serverDatasetBlobType].A
          ? {
              category,
              type: serverDatasetBlobType as IDatasetBlob['type'],
              components: convertDatasetComponents(
                serverDatasetDiff[category][serverDatasetBlobType].A,
                serverDatasetBlobType as IDatasetBlob['type']
              ),
            }
          : undefined,
        blobB: serverDatasetDiff[category][serverDatasetBlobType].B
          ? {
              category,
              type: serverDatasetBlobType as IDatasetBlob['type'],
              components: convertDatasetComponents(
                serverDatasetDiff[category][serverDatasetBlobType].B,
                serverDatasetBlobType as IDatasetBlob['type']
              ),
            }
          : undefined,
      } as IDatasetBlobDiff;
    }

    default:
      exhaustiveCheck(diffType, '');
  }
};

const convertDatasetComponents = (
  serverComponents: any,
  datasetBlobType: IDatasetBlob['type']
) => {
  switch (datasetBlobType) {
    case 'path':
      return serverComponents.map(convertServerDatasetPathComponent);

    case 's3':
      return serverComponents.map(convertServerDatasetS3Component);

    default:
      exhaustiveCheck(datasetBlobType, '');
  }
};

export default convertServerDatasetDiff;
