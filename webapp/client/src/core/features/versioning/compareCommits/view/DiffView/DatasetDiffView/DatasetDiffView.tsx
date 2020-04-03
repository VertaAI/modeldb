import * as React from 'react';

import { IDatasetBlobDiff } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import matchType from 'core/shared/utils/matchType';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import matchBy from 'core/shared/utils/matchBy';

import PathComponentsDiff from './PathComponentsDiff/PathComponentsDiff';

const DatasetDiffView = ({ diff }: { diff: IDatasetBlobDiff }) => {
  const title = matchType(
    {
      s3: () => 'S3 Dataset',
      path: () => 'Path Dataset',
    },
    diff.type
  );

  const pathComponentsDiff = matchBy(diff, 'type')({
    path: pathDiff =>
      pathDiff.data.components.map(pathComponent => pathComponent),
    s3: s3Diff => s3Diff.data.components.map(({ path }) => path),
  });

  return (
    <BlobDataBox title={title}>
      <PathComponentsDiff diff={pathComponentsDiff} />
    </BlobDataBox>
  );
};

export default DatasetDiffView;
