import * as React from 'react';

import styles from './ModelInput.module.css';

class ModelInput extends React.PureComponent {
  public render() {
    return (
      <div className={styles.model_input}>
        <div className={styles.title}>How to use</div>
        <div className={styles.fields}>
          <div className={styles.header}>Name</div>
          <div className={styles.header}>Type</div>
          <div className={styles.content}>Cell Name</div>
          <div className={styles.content}>Cell Type</div>
          <div className={styles.content}>Cell Name</div>
          <div className={styles.content}>Cell Type</div>
        </div>
      </div>
    );
  }
}

export default ModelInput;
