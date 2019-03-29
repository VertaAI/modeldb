import cn from 'classnames';
import * as React from 'react';

import styles from './Button.module.css';

interface IProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  to?: string;
  // todo rename
  variant?: 'like-link' | 'default';
  fullWidth?: boolean;
  onClick?(): void;
}

class Button extends React.PureComponent<IProps> {
  public render() {
    const {
      children,
      to,
      fullWidth = false,
      variant = 'default',
      onClick,
    } = this.props;
    const Elem = (props: any) =>
      to ? (
        <a href={to} {...props}>
          {children}
        </a>
      ) : (
        <button {...props}>{children}</button>
      );
    return (
      <Elem
        className={cn(styles.button, {
          [styles.like_link]: variant === 'like-link',
          [styles.default]: variant === 'default',
          [styles.full_width]: fullWidth,
        })}
        onClick={onClick}
      >
        {children}
      </Elem>
    );
  }
}

export default Button;
