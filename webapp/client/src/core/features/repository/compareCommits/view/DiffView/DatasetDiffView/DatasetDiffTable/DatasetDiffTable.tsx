import React from 'react';

import { IDatasetBlob } from 'core/shared/models/Repository/Blob/DatasetBlob';
import { DiffType } from 'core/shared/models/Repository/Blob/Diff';
import { getObjsPropsDiff } from 'core/shared/utils/collection';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import matchType from 'core/shared/utils/matchType';

import styles from './DatasetDiffTable.module.css';
import Table, { IRow } from './Table/Table';

interface ILocalProps {
  diffType: DiffType;
  blobA?: IDatasetBlob;
  blobB?: IDatasetBlob;
}

const DatasetDiffTable: React.FC<ILocalProps> = ({
  blobA,
  blobB,
  diffType,
}) => {
  const convertedBlobAData = blobA
    ? (() => {
        switch (blobA.type) {
          case 's3':
            return blobA.components.map(({ path }) => path);
          case 'path':
            return blobA.components;
          default:
            return exhaustiveCheck(blobA, '');
        }
      })()
    : undefined;

  const convertedBlobBData =
    diffType === 'updated' && blobB
      ? (() => {
          switch (blobB.type) {
            case 's3':
              return blobB.components.map(({ path }) => path);
            case 'path':
              return blobB.components;
            default:
              return exhaustiveCheck(blobB, '');
          }
        })()
      : undefined;

  const title = blobA
    ? matchType(
        {
          s3: () => 'S3 Dataset',
          path: () => 'Path Dataset',
        },
        blobA.type
      )
    : '';

  const rows: IRow[] = (convertedBlobAData || convertedBlobBData || [])
    .map((d, index) => {
      if (diffType === 'updated') {
        if (blobA && !blobB) {
          return {
            data: d,
            diffInfo: {
              status: 'deleted',
              data: {},
            },
            isFirstCommit: true,
          };
        }
        if (!blobA && blobB) {
          return {
            data: d,
            diffInfo: {
              status: 'added',
              data: {},
            },
            isFirstCommit: false,
          };
        }

        return [
          {
            data: d,
            isFirstCommit: true,
            diffInfo: {
              status: 'updated',
              data: getObjsPropsDiff(
                d,
                (convertedBlobBData || convertedBlobAData || [])[index] || {}
              ),
            },
          },
          {
            data: (convertedBlobBData || convertedBlobAData || [])[index],
            isFirstCommit: false,
            diffInfo: {
              status: 'updated',
              data: getObjsPropsDiff(
                d,
                (convertedBlobBData || convertedBlobAData || [])[index] || {}
              ),
            },
          },
        ];
      }

      return {
        data: d,
        diffInfo: {
          status: diffType,
          data: {},
        },
        isFirstCommit: diffType === 'deleted',
      };
    })
    .flat();

  return (
    <div className={styles.root}>
      <div className={styles.title}>{title}</div>
      <div className={styles.table}>
        <Table rows={rows} />
      </div>
    </div>
  );
};

export default DatasetDiffTable;
