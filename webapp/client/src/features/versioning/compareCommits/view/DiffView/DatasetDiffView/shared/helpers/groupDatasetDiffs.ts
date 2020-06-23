import {
  highlightAllProperties,
  highlightModifiedProperties,
  IObjectToObjectWithDiffColor,
} from '../../../../model';

import { IElementDiff } from 'shared/models/Versioning/Blob/Diff';

export const groupDatasetDiffs = <D>(
  diffs: Array<IElementDiff<D>>
): Array<Array<IObjectToObjectWithDiffColor<D>>> => {
  const groups = diffs.map(component => {
    if (component.diffType === 'added') {
      return [highlightAllProperties('B', component.B)];
    }
    if (component.diffType === 'deleted') {
      return [highlightAllProperties('A', component.A)];
    }
    if (component.diffType === 'conflicted') {
      return component.C
        ? [
            highlightAllProperties('A', component.A),
            highlightAllProperties('C', component.C),
            highlightAllProperties('B', component.B),
          ]
        : [
            highlightModifiedProperties('A', component.A, component.B),
            highlightModifiedProperties('B', component.B, component.A),
          ];
    }
    return [
      highlightModifiedProperties('A', component.A, component.B),
      highlightModifiedProperties('B', component.B, component.A),
    ];
  });

  return groups;
};
