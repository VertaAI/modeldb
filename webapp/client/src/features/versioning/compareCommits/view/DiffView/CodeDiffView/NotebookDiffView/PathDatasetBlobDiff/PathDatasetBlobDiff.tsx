import * as React from 'react';

import { IPathDatasetComponentBlobDiff } from 'shared/models/Versioning/Blob/DatasetBlob';
import { BlobDataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import PathComponentsDiff from '../../../DatasetDiffView/PathComponentsDiff/PathComponentsDiff';

interface ILocalProps {
  diff: IPathDatasetComponentBlobDiff;
}

const PathDatasetBlobDiff = ({ diff }: ILocalProps) => {
  return (
    <BlobDataBox title="Path">
      <PathComponentsDiff diff={[diff]} />
    </BlobDataBox>
  );
};

export default PathDatasetBlobDiff;
