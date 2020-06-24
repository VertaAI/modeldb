import cn from 'classnames';
import * as React from 'react';

import styles from './IdView.module.css';

interface ILocalProps {
  value: string;
  title?: string;
  additionalClassName?: string;
  sliceStringUpto?: number;
  dataTest?: string;
}

const IdView = React.memo(
  ({
    value,
    additionalClassName,
    title,
    sliceStringUpto,
    dataTest,
  }: ILocalProps) => {
    return (
      <span
        className={cn(styles.root, additionalClassName)}
        title={title}
        data-test={dataTest}
      >
        {sliceStringUpto ? value.slice(0, sliceStringUpto) : value}
      </span>
    );
  }
);

export default IdView;
