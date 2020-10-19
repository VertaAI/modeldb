import * as R from 'ramda';

import { IBlob, blobCategories } from 'shared/models/Versioning/Blob/Blob';
import { Diff, DiffType } from 'shared/models/Versioning/Blob/Diff';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import matchType from 'shared/utils/matchType';

import { convertServerCodeDiff } from './convertServerCodeDiff';
import { convertServerConfigDiff } from './convertServerConfigDiff';
import convertServerDatasetDiff from './convertServerDatasetDiff';
import { convertServerEnvironmentDiff } from './convertServerEnvironmentDiff';

export const convertServerDiffsToClient = (serverDiff: any[]): Diff[] => {
  return (serverDiff || [])
    .filter((diff: any) => !R.isEmpty(diff))
    .map(convertServerDiffToClient);
};

export const convertServerDiffToClient = (serverDiff: any): Diff => {
  const serverDiffCategory = Object.keys(
    R.omit(['location', 'status'], serverDiff)
  )[0];
  const category: IBlob['data']['category'] =
    serverDiffCategory in blobCategories
      ? (serverDiffCategory as IBlob['data']['category'])
      : 'unknown';

  switch (category) {
    case 'code': {
      return convertServerCodeDiff(serverDiff);
    }

    case 'config': {
      return convertServerConfigDiff(serverDiff);
    }

    case 'environment': {
      return convertServerEnvironmentDiff(serverDiff);
    }

    case 'dataset': {
      return convertServerDatasetDiff(serverDiff);
    }

    case 'unknown': {
      const diffType: DiffType = serverDiff.status
        ? matchType(
            {
              MODIFIED: () => 'modified',
              DELETED: () => 'deleted',
              ADDED: () => 'added',
              CONFLICT: () => 'conflicted',
            },
            serverDiff.status
          )
        : 'added';
      switch (diffType) {
        case 'added':
        case 'deleted':
          return {
            diffType,
            category: 'unknown',
            type: 'unknown',
            location: serverDiff.location,
            blob: serverDiff[serverDiffCategory],
          } as any;

        case 'modified': {
          return {
            diffType,
            category: 'unknown',
            type: 'unknown',
            location: serverDiff.location,
            blobA: serverDiff[serverDiffCategory].A,
            blobB: serverDiff[serverDiffCategory].B,
          } as any;
        }

        case 'conflicted': {
          return {
            diffType,
            category: 'unknown',
            type: 'unknown',
            location: serverDiff.location,
            blobA: serverDiff[serverDiffCategory].A,
            blobB: serverDiff[serverDiffCategory].B,
            blobC: serverDiff[serverDiffCategory].C,
          } as any;
        }

        default:
          exhaustiveCheck(diffType, '');
      }
      break;
    }

    default: {
      exhaustiveCheck(category, '');
    }
  }
};
