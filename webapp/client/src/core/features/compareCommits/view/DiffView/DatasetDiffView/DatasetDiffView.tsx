import * as React from 'react';

import { IDatasetBlobDiff } from 'core/shared/models/Repository/Blob/DatasetBlob';
import matchBy from 'core/shared/utils/matchBy';

import DatasetDiffTable from './DatasetDiffTable/DatasetDiffTable';

const DatasetDiffView = ({ diff }: { diff: IDatasetBlobDiff }) => {
  return matchBy(diff, 'diffType')({
    added: d => <DatasetDiffTable blobA={d.blob} diffType={diff.diffType} />,
    deleted: d => <DatasetDiffTable blobA={d.blob} diffType={diff.diffType} />,
    updated: d => (
      <DatasetDiffTable
        blobA={d.blobA}
        blobB={d.blobB}
        diffType={diff.diffType}
      />
    ),
  });
};

export default DatasetDiffView;
