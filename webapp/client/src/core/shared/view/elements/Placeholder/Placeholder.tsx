import * as React from 'react';
import cn from 'classnames';

import styles from './Placeholder.module.css';

interface ILocalProps {
  dataTest?: string;
  withoutCentering?: boolean;
  children: React.ReactNode;
}

const Placeholder = (props: ILocalProps) => {
  return (
    <div
      className={cn(styles.root, {
        [styles.withoutCentering]: props.withoutCentering,
      })}
      data-test={props.dataTest}
    >
      {props.children}
    </div>
  );
};

export default Placeholder;
