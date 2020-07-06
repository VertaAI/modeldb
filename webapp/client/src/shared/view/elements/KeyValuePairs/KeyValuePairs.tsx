import * as React from 'react';

import { IKeyValuePair } from 'shared/models/Common';

import styles from './KeyValuePairs.module.css';

interface ILocalProps {
  data: Array<IKeyValuePair<string>>;
  getStyles?: (
    pair: IKeyValuePair<string>,
    i: number
  ) =>
    | { rootStyles?: React.CSSProperties; valueStyles?: React.CSSProperties }
    | undefined;
}

const KeyValuePairs = ({ data, getStyles = () => undefined }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {data.map((pair, i) => {
        const pairStyles = getStyles(pair, i) || {};
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
