import * as React from 'react';
import cn from 'classnames';

import { NA } from 'shared/view/elements/PageComponents';

import styles from './PopupComparedEntities.module.css';

export function PopupComparedEntities<T>({ entities, children }: { entities: T[]; children: (item: Exclude<T, null | undefined>) => React.ReactNode }) {
    return (
      <div className={styles.entities}>
        {entities.map((item, i) => item ? (
          <div className={styles.entity} key={i}>
            {children(item as Exclude<T, null | undefined>)}
          </div>
        ) : (
          <div className={cn(styles.entity, { [styles.eempty]: true })} key={i}>
            {NA}
          </div>
        ))}
      </div>
    );
  };
