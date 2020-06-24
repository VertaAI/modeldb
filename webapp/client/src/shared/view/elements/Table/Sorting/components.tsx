import cn from 'classnames';
import React from 'react';

import { Icon } from '../../Icon/Icon';
import { CustomSortLabel } from '../types';
import styles from './styles.module.css';
import { SortLabelDirection } from './types';

export const SortLabel = ({
  onSort,
  direction,
  children,
  customSortLabel,
}: {
  onSort: () => void;
  direction: SortLabelDirection;
  children: React.ReactNode;
  customSortLabel?: CustomSortLabel;
}) => {
  if (customSortLabel) {
    const Component = customSortLabel;
    return <Component>{children}</Component>;
  }

  return (
    <div className={styles.sortLabel} data-test="sort-label">
      {children}
      <div
        className={styles.sortIcon}
        onClick={onSort}
        data-test="sort-label-icon"
      >
        <div className={styles.field_sorting_indicator}>
          <Icon
            type="arrow-up-lite"
            className={cn(styles.header_sorting_indicator_asc, {
              [styles.selected]: direction === 'asc',
            })}
          />
          <Icon
            type="arrow-down-lite"
            className={cn(styles.header_sorting_indicator_desc, {
              [styles.selected]: direction === 'desc',
            })}
          />
        </div>
      </div>
    </div>
  );
};
