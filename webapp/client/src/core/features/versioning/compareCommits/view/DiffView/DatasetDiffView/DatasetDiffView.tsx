import * as React from 'react';

import { IDatasetBlobDiff, IDatasetBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import matchBy from 'core/shared/utils/matchBy';

import DatasetDiffTable from './DatasetDiffTable/DatasetDiffTable';
import { ComparedCommitType, getAData, getBData } from 'core/shared/models/Versioning/Blob/Diff';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

const DatasetDiffView = ({ diff }: { diff: IDatasetBlobDiff }) => {
  return matchBy(diff, 'diffType')({
    added: d => {
      return <DatasetDiffTable blobA={getDatasetBlobFromDiff('B', d)} diffType={diff.diffType} />;
    },
    deleted: d => <DatasetDiffTable blobA={getDatasetBlobFromDiff('A', d)} diffType={diff.diffType} />,
    updated: d => {
      return (
        <DatasetDiffTable
          blobA={getDatasetBlobFromDiff('A', d)}
          blobB={getDatasetBlobFromDiff('B', d)}
          diffType={diff.diffType}
        />
      );
    },
  });
};

const getDatasetBlobFromDiff = (type: ComparedCommitType, diff: IDatasetBlobDiff): IDatasetBlob => {
  switch (diff.type) {
    case 'path': {
      return {
        type: 'path',
        category: 'dataset',
        components: diff.data.components
          .map((c) => {
            return type === 'A' ? getAData(c)! : getBData(c)!;
          })
          .filter(Boolean),
      };      
    }
    case 's3': {
      return {
        type: 's3',
        category: 'dataset',
        components: diff.data.components
          .map((c) => {
            return {
              path: type === 'A' ? getAData(c.path)! : getBData(c.path)!,
            };
          })
          .filter((c) => Boolean(c.path)),
      };
    }
    default: return exhaustiveCheck(diff, '');
  }
};

export default DatasetDiffView;
