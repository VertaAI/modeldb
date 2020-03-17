import * as R from 'ramda';

import { IBlob, blobCategories } from 'core/shared/models/Repository/Blob/Blob';
import { Diff, DiffType } from 'core/shared/models/Repository/Blob/Diff';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import matchType from 'core/shared/utils/matchType';

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

  const diffType: DiffType = serverDiff.status
    ? matchType(
        {
          MODIFIED: () => 'updated',
          DELETED: () => 'deleted',
          ADDED: () => 'added',
        },
        serverDiff.status
      )
    : 'added';

  switch (category) {
    case 'code': {
      return convertServerCodeDiff(serverDiff, diffType);
    }

    case 'config': {
      return convertServerConfigDiff(serverDiff, diffType);
    }

    case 'environment': {
      return convertServerEnvironmentDiff(serverDiff, diffType);
    }

    case 'unknown': {
      switch (diffType) {
        case 'added':
        case 'deleted':
          return {
            diffType,
            category: 'unknown',
            type: 'unknown',
            location: serverDiff.location,
            blob: serverDiff[serverDiffCategory],
          };

        case 'updated': {
          return {
            diffType,
            category: 'unknown',
            type: 'unknown',
            location: serverDiff.location,
            blobA: serverDiff[serverDiffCategory].A,
            blobB: serverDiff[serverDiffCategory].B,
          };
        }

        default:
          exhaustiveCheck(diffType, '');
      }
    }

    case 'dataset': {
      return convertServerDatasetDiff(serverDiff, diffType);
    }

    default: {
      exhaustiveCheck(category, '');
    }
  }
};
