import { IBlob } from 'shared/models/Versioning/Blob/Blob';
import { IDatasetBlob } from 'shared/models/Versioning/Blob/DatasetBlob';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';

import { convertServerCodeBlobToClient } from './CodeBlob';
import { convertConfigBlobToClient } from './ConfigBlob';
import {
  convertPathDatasetBlob,
  convertServerDatasetPathComponent,
  convertServerDatasetS3Component,
} from './DatasetBlob';
import { convertServerEnvironmentBlob } from './EnviromentBlob';

export const convertServerBlobToClient = (serverBlob: any): IBlob => {
  try {
    if (serverBlob.dataset) {
      if (serverBlob.dataset.s3) {
        return {
          type: 'blob',
          data: convertServerBlobDataToClient({
            serverBlobData: serverBlob.dataset.s3,
            category: 'dataset',
            datasetBlobType: 's3',
          }),
        };
      }
      if (serverBlob.dataset.path) {
        return {
          type: 'blob',
          data: convertPathDatasetBlob(serverBlob.dataset.path),
        };
      }
    }

    const codeBlob = serverBlob.code
      ? convertServerCodeBlobToClient(serverBlob.code)
      : undefined;
    if (codeBlob) {
      return { type: 'blob', data: codeBlob };
    }

    const environmentBlob = serverBlob.environment
      ? convertServerEnvironmentBlob(serverBlob.environment)
      : undefined;
    if (environmentBlob) {
      return { type: 'blob', data: environmentBlob };
    }

    const configBlob = serverBlob.config
      ? convertConfigBlobToClient(serverBlob.config)
      : undefined;
    if (configBlob) {
      return { type: 'blob', data: configBlob };
    }

    return {
      type: 'blob',
      data: {
        category: 'unknown',
        data: convertServerBlobDataToClient({
          serverBlobData: serverBlob,
          category: 'unknown',
        }),
      },
    };
  } catch (e) {
    return {
      type: 'blob',
      data: convertServerBlobDataToClient({
        serverBlobData: serverBlob,
        category: 'unknown',
      }),
    };
  }
};

export const convertServerBlobDataToClient = ({
  serverBlobData,
  category,
  datasetBlobType,
}: {
  serverBlobData: any;
  category: IBlob['data']['category'];
  datasetBlobType?: IDatasetBlob['type'];
}): IBlob['data'] => {
  switch (category) {
    case 'dataset': {
      switch (datasetBlobType) {
        case 'path': {
          return {
            category: 'dataset',
            type: 'path',
            data: {
              components: serverBlobData.components.map(
                convertServerDatasetPathComponent
              ),
            },
          };
        }

        case 's3': {
          return {
            category: 'dataset',
            type: 's3',
            data: {
              components: serverBlobData.components.map(
                convertServerDatasetS3Component
              ),
            },
          };
        }

        case undefined: {
          return {
            category: 'unknown',
            data: serverBlobData,
          };
        }

        default:
          exhaustiveCheck(datasetBlobType, '');
      }
      break;
    }

    case 'config':
    case 'environment':
    case 'code':
    case 'unknown': {
      return {
        category: 'unknown',
        data: serverBlobData,
      };
    }
    default:
      exhaustiveCheck(category, '');
  }
};
