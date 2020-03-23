import * as React from 'react';

import { IKeyValuePair } from 'core/shared/models/Common';

import styles from './KeyValuePairs.module.css';

interface ILocalProps {
  data: Array<IKeyValuePair<string>>;
  getRootStyles?: (
    pair: IKeyValuePair<string>
  ) => React.CSSProperties | undefined;
}

const KeyValuePairs = ({
  data,
  getRootStyles = () => undefined,
}: ILocalProps) => {
  return (
    <div className={styles.root}>
      {data.map(pair => (
        <div className={styles.pair} key={pair.key} style={getRootStyles(pair)}>
          <span className={styles.pair__key} title={pair.key}>
            {pair.key}
          </span>
          <span className={styles.pair__value} title={pair.value}>
            {pair.value}
          </span>
        </div>
      ))}
    </div>
  );
};

export default KeyValuePairs;
