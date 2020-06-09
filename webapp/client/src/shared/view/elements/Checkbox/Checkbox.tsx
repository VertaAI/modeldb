import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import styles from './Checkbox.module.css';
import { Icon } from '../Icon/Icon';

interface ILocalProps {
  value: boolean;
  size: 'small' | 'medium';
  id?: string;
  disabled?: boolean;
  isRounded?: boolean;
  theme?: 'red';
  name?: string;
  dataTest?: string;
  onChange?(value: boolean): void;
  onChangeWithEvent?(event: any): void;
}

class Checkbox extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      value,
      dataTest,
      size,
      disabled,
      id,
      name,
      isRounded,
      theme,
    } = this.props;
    return (
      <label
        className={cn(styles.checkbox, {
          [styles.size_small]: size === 'small',
          [styles.size_medium]: size === 'medium',
          [styles.rounded]: Boolean(isRounded),
          [styles.theme_red]: theme === 'red',
        })}
      >
        <input
          className={styles.real_checkbox}
          type="checkbox"
          checked={value}
          disabled={disabled}
          id={id}
          name={name}
          data-test={dataTest}
          onChange={this.onChange}
        />
        <span className={styles.checkmark}>
          {value && <Icon type="checkmark" />}
        </span>
      </label>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (this.props.onChange) {
      this.props.onChange(e.target.checked);
    }
    if (this.props.onChangeWithEvent) {
      this.props.onChangeWithEvent(e);
    }
  }
}

export default Checkbox;
