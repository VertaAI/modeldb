import * as React from 'react';

import styles from './NoEntitiesStub.module.css';

interface ILocalProps {
  entitiesText: string;
}

class NoEntitiesStub extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <div className={styles.root}>
        <span className={styles.text}>No {this.props.entitiesText}</span>
      </div>
    );
  }
}

export default NoEntitiesStub;
