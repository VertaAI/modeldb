import * as React from 'react';
import { bind } from 'decko';

import styles from './Checkbox.module.css';

interface IProps {
  value: boolean;
  onChange(value: boolean): void;
}

class Checkbox extends React.PureComponent<IProps> {
  public render() {
    const { value } = this.props;
    return (
      <label className={styles.checkbox}>
        <input className={styles.real_checkbox} type="checkbox" checked={value} onChange={this.onChange} />
        <div className={styles.slider} />
      </label>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    this.props.onChange(e.currentTarget.checked);
  }
}

export default Checkbox;
