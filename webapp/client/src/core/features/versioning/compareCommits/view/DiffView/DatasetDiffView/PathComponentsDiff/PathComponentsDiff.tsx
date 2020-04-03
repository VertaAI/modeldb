import React from 'react';

import { IPathDatasetComponentBlobDiff } from 'core/shared/models/Versioning/Blob/DatasetBlob';

import Table from './Table/Table';
import {
  highlightAllProperties,
  highlightModifiedProperties,
} from '../../../model';

interface ILocalProps {
  diff: IPathDatasetComponentBlobDiff[];
}

const PathComponentsDiff: React.FC<ILocalProps> = ({ diff }) => {
  const rows = diff.flatMap(pathComponent => {
    if (pathComponent.diffType === 'added') {
      return [highlightAllProperties('B', pathComponent.B)];
    }
    if (pathComponent.diffType === 'deleted') {
      return [highlightAllProperties('A', pathComponent.A)];
    }
    return [
      highlightModifiedProperties('A', pathComponent.A, pathComponent.B),
      highlightModifiedProperties('B', pathComponent.B, pathComponent.A),
    ];
  });
  return <Table rows={rows} />;
};

export default PathComponentsDiff;
