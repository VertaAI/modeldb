import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { Icon } from '../Icon/Icon';
import styles from './TextInput.module.css';

interface ILocalProps {
  size: 'small' | 'medium';
  name?: string;
  isDisabled?: boolean;
  defaultValue?: string;
  value?: string;
  placeholder?: string;
  icon?: 'search';
  leftContent?: React.ReactNode;
  dataTest?: string;
  theme?: 'light' | 'dark';
  onChange?(value: string): void;
  onChangeWithEvent?(e: any): void;
  onKeyUp?(e: React.KeyboardEvent<HTMLInputElement>): void;
  onBlur?(e: React.FocusEvent<HTMLInputElement>): void;
  onClick?(): void;
  onFocus?(e: React.FocusEvent<HTMLInputElement>): void;
  onInputRef?(ref: HTMLInputElement): void;
}

interface ILocalState {
  isFocused: boolean;
}

class TextInput extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = { isFocused: false };

  public render() {
    const {
      value,
      defaultValue,
      isDisabled,
      size,
      placeholder,
      icon,
      dataTest,
      leftContent,
      name,
      theme = 'light',
      onKeyUp,
      onClick,
      onInputRef,
    } = this.props;
    const { isFocused } = this.state;
    return (
      <div
        className={cn(styles.root, {
          [styles.size_small]: size === 'small',
          [styles.size_medium]: size === 'medium',
          [styles.with_icon]: Boolean(icon),
          [styles.focused]: Boolean(isFocused),
          [styles.theme_dark]: theme === 'dark',
          [styles.theme_light]: theme === 'light',
        })}
      >
        {leftContent && (
          <div className={styles.left_content}>{leftContent}</div>
        )}
        <input
          className={cn(styles.field, {
            [styles.disabled]: isDisabled === true,
          })}
          ref={onInputRef}
          type="input"
          defaultValue={defaultValue}
          value={value}
          name={name}
          placeholder={placeholder}
          data-test={dataTest}
          disabled={isDisabled}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
          onChange={this.onChange}
          onKeyUp={onKeyUp}
          onClick={onClick}
        />
        {icon && <Icon type="search" className={styles.icon} />}
      </div>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (this.props.onChange) {
      this.props.onChange(e.target.value);
    }
    if (this.props.onChangeWithEvent) {
      this.props.onChangeWithEvent(e);
    }
  }

  @bind
  private onFocus(e: React.FocusEvent<HTMLInputElement>) {
    this.setState({ isFocused: true });
    if (this.props.onFocus) {
      this.props.onFocus(e);
    }
  }

  @bind
  private onBlur(e: React.FocusEvent<HTMLInputElement>) {
    this.setState({ isFocused: false });
    if (this.props.onBlur) {
      this.props.onBlur(e);
    }
  }
}

export default TextInput;
