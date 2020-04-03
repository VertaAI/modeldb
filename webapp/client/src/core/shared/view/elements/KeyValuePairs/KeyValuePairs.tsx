import * as React from 'react';

import { IKeyValuePair } from 'core/shared/models/Common';

import styles from './KeyValuePairs.module.css';

interface ILocalProps {
  data: Array<IKeyValuePair<string>>;
  getStyles?: (
    pair: IKeyValuePair<string>
  ) =>
    | { rootStyles?: React.CSSProperties; valueStyles?: React.CSSProperties }
    | undefined;
}

const KeyValuePairs = ({ data, getStyles = () => undefined }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {data.map(pair => {
        const pairStyles = getStyles(pair) || {};
        return (
          <div
            className={styles.pair}
            key={pair.key}
            style={pairStyles.rootStyles}
          >
            <span className={styles.pair__key} title={pair.key}>
              {pair.key}
            </span>
            <span
              className={styles.pair__value}
              title={pair.value}
              style={pairStyles.valueStyles}
            >
              {pair.value}
            </span>
          </div>
        );
      })}
    </div>
  );
};

export default KeyValuePairs;
