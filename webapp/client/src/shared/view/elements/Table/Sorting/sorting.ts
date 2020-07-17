import * as R from 'ramda';
import { useState } from 'react';

import { ColumnDefinition } from '../types';
import { SortInfo } from './types';

type OptionalSortInfo = SortInfo | null;

export function useSortInfo(): [OptionalSortInfo, (type: string) => void] {
  const [sortInfo, changeSortInfo] = useState<OptionalSortInfo>(null);

  const changeSortedType = (type: string) => {
    if (!sortInfo) {
      changeSortInfo({
        type,
        isAscDirection: false,
      });
    } else if (sortInfo.type === type) {
      changeSortInfo({
        type,
        isAscDirection: !sortInfo.isAscDirection,
      });
    } else {
      changeSortInfo({
        type,
        isAscDirection: false,
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
  sortInfo: OptionalSortInfo;
}): T[] {
  if (!sortInfo) {
    return dataRows;
  }

  const columnDefinition = columnDefinitions.find(
    c => c.type === sortInfo.type
  );

  if (columnDefinition && 'getValue' in columnDefinition && columnDefinition.getValue) {
    const sortedRows = R.sortBy(columnDefinition.getValue)(dataRows);

    const ret = sortInfo.isAscDirection ? sortedRows : R.reverse(sortedRows);

    return ret;
  }

  throw new Error('Incorrect type in columnDefinitions');
}
