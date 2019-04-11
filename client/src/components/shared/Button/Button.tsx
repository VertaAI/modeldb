import cn from 'classnames';
import * as React from 'react';

import styles from './Button.module.css';

interface ILocalProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  to?: string;
  variant?: 'like-link' | 'default';
  fullWidth?: boolean;
  onClick?(): void;
}

class Button extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      to,
      fullWidth = false,
      variant = 'default',
      onClick,
    } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? (
        <a href={to} {...props}>
          {children}
        </a>
      ) : (
        // @ts-ignore
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
