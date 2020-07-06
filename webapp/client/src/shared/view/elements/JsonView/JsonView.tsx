import React from 'react';
import ReactJson from 'react-json-view';

import styles from './JsonView.module.css';

interface ILocalProps {
  object: object;
}

export const JsonView: React.FC<ILocalProps> = ({ object }) => {
  return (
    <div className={styles.root}>
      <ReactJson
        src={object}
        enableClipboard={false}
        displayDataTypes={false}
      />
    </div>
  );
};
