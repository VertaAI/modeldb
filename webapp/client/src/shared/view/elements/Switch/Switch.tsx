import { bind } from 'decko';
import * as React from 'react';

import styles from './Switch.module.css';

interface ILocalProps {
  value: boolean;
  dataTest?: string;
  onChange(value: boolean): void;
}

const noop = () => {};

class Switch extends React.PureComponent<ILocalProps> {
  public render() {
    const { value, dataTest = 'switch' } = this.props;
    return (
      <div className={styles.switch} onClick={this.onChange}>
        <input
          className={styles.real_checkbox}
          type="checkbox"
          checked={value}
          data-test={dataTest}
          onChange={noop}
        />
        <div className={styles.slider} />
      </div>
    );
  }

  @bind
  private onChange() {
    this.props.onChange(!this.props.value);
  }
}

export default Switch;
