import { ComparedCommitType } from 'shared/models/Versioning/Blob/Diff';

import { getCssDiffColorByCommitType } from '../../model';

export function makeHighlightCellBackground<T>() {
  function highlightCellBackground(
    pred: (settings: {
      data: T;
      comparedCommitType: ComparedCommitType;
    }) => boolean
  ) {
    return (settings: { data?: T; comparedCommitType: ComparedCommitType }) => {
      return settings.data &&
        pred({
          data: settings.data,
          comparedCommitType: settings.comparedCommitType,
        })
        ? {
            backgroundColor: getCssDiffColorByCommitType(
              settings.comparedCommitType
            ),
          }
        : {};
    };
  }
  return highlightCellBackground;
}
