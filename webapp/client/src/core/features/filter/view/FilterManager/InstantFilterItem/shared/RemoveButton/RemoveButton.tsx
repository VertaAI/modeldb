import * as React from 'react';

import styles from './RemoveButton.module.css';

interface ILocalProps {
  onRemove(): void;
}

class RemoveButton extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <div
        className={styles.root}
        onClick={this.props.onRemove}
        data-test="delete-filter-button"
      >
        x
      </div>
    );
  }
}

export default RemoveButton;
