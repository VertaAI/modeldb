import * as R from 'ramda';
import { useState } from 'react';

import { ColumnDefinition } from '../types';

type SortInfo = {
  type: string;
  isReversed: boolean;
} | null;

export function useSortInfo(): [SortInfo, (type: string) => void] {
  const [sortInfo, changeSortInfo] = useState<SortInfo>(null);

  const changeSortedType = (type: string) => {
    if (!sortInfo) {
      changeSortInfo({
        type,
        isReversed: false,
      });
    } else if (sortInfo.type === type) {
      changeSortInfo({
        type,
        isReversed: !sortInfo.isReversed,
      });
    } else {
      changeSortInfo({
        type,
        isReversed: false,
      });
    }
  };

  return [sortInfo, changeSortedType];
}

export function sortDataRows<T>({
  dataRows,
  columnDefinitions,
  sortInfo,
}: {
  columnDefinitions: Array<ColumnDefinition<T>>;
  dataRows: T[];
  sortInfo: SortInfo;
}): T[] {
  if (!sortInfo) {
    return dataRows;
  }

  const columnDefinition = columnDefinitions.find(
    c => c.type === sortInfo.type
  );

  if (columnDefinition && 'getValue' in columnDefinition) {
    const sortedRows = R.sortBy(columnDefinition.getValue)(dataRows);

    const ret = sortInfo.isReversed ? sortedRows : R.reverse(sortedRows);

    return ret;
  }

  throw new Error('Incorrect type in columnDefinitions');
}
