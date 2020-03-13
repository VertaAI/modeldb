import * as React from 'react';

import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './NoResultsStub.module.css';

class NoResultsStub extends React.PureComponent {
  public render() {
    return (
      <div className={styles.root}>
        <Icon className={styles.icon} type="search" />
        <span className={styles.text}>No results</span>
      </div>
    );
  }
}

export default NoResultsStub;
