import { bind } from 'decko';
import * as React from 'react';

import styles from './Switch.module.css';

interface ILocalProps {
  value: boolean;
  onChange(value: boolean): void;
}

class Switch extends React.PureComponent<ILocalProps> {
  public render() {
    const { value } = this.props;
    return (
      <label className={styles.switch}>
        <input
          className={styles.real_checkbox}
          type="checkbox"
          checked={value}
          onChange={this.onChange}
        />
        <div className={styles.slider} />
      </label>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    this.props.onChange(e.currentTarget.checked);
  }
}

export default Switch;
