import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import Icon from '../Icon/Icon';
import styles from './TextInput.module.css';

interface ILocalProps {
  defaultValue?: string;
  value?: string;
  placeholder?: string;
  size: 'small' | 'medium';
  icon?: 'search';
  onChange?(value: string): void;
  onKeyUp?(e: React.KeyboardEvent<HTMLInputElement>): void;
  onBlur?(e: React.FocusEvent<HTMLInputElement>): void;
  onClick?(): void;
}

class TextInput extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      value,
      defaultValue,
      size,
      placeholder,
      icon,
      onKeyUp,
      onBlur,
      onClick,
    } = this.props;
    return (
      <div
        className={cn(styles.input, {
          [styles.size_small]: size === 'small',
          [styles.size_medium]: size === 'medium',
          [styles.with_icon]: Boolean(icon),
        })}
      >
        <input
          className={styles.field}
          type="input"
          defaultValue={defaultValue}
          value={value}
          placeholder={placeholder}
          onChange={this.onChange}
          onKeyUp={onKeyUp}
          onBlur={onBlur}
          onClick={onClick}
        />
        {icon && <Icon type="search" className={styles.icon} />}
      </div>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (this.props.onChange) {
      this.props.onChange(e.currentTarget.value);
    }
  }
}

export default TextInput;
