import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import styles from './Checkbox.module.css';

interface ILocalProps {
  value: boolean;
  size: 'small' | 'medium';
  id?: string;
  onChange(value: boolean): void;
}

class Checkbox extends React.PureComponent<ILocalProps> {
  public render() {
    const { value, size, id } = this.props;
    return (
      <label
        className={cn(styles.checkbox, {
          [styles.size_small]: size === 'small',
          [styles.size_medium]: size === 'medium',
        })}
      >
        <input
          className={styles.real_checkbox}
          type="checkbox"
          checked={value}
          id={id}
          onChange={this.onChange}
        />
        <span className={styles.checkmark} />
      </label>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    this.props.onChange(e.currentTarget.checked);
  }
}

export default Checkbox;
