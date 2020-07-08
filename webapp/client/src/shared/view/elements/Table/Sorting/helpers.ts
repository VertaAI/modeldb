import { useMemo } from 'react';
import { SortInfo, SortLabelDirection } from './types';

export const useLabelDirection = ({
  sortInfo,
  type,
}: {
  sortInfo: SortInfo;
  type: string;
}): SortLabelDirection =>
  useMemo(() => {
    if (!sortInfo || sortInfo.type !== type) {
      return null;
    }

    return sortInfo.isReversed ? 'asc' : 'desc';
  }, [sortInfo]);
