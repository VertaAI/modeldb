import cn from 'classnames';
import * as React from 'react';

import styles from './Button.module.css';
import githubSrc from './imgs/github.svg';

interface ILocalProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  to?: string;
  variant?: 'like-link' | 'default';
  fullWidth?: boolean;
  size?: 'default' | 'large'; // todo maybe default rename to medium
  icon?: Icon;
  textTransform?: 'default' | 'none';
  onClick?(): void;
}

type Icon = 'github';

class Button extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      to,
      icon,
      fullWidth = false,
      variant = 'default',
      size = 'default',
      textTransform = 'default',
      onClick,
    } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? <a href={to} {...props} /> : <button {...props} />;
    return (
      <Elem
        className={cn(styles.button, {
          [styles.like_link]: variant === 'like-link',
          [styles.default]: variant === 'default',
          [styles.full_width]: fullWidth,
          [styles.size_default]: size === 'default',
          [styles.size_large]: size === 'large',
          [styles.text_transform_default]: textTransform === 'default',
          [styles.text_transform_none]: textTransform === 'none',
        })}
        onClick={onClick}
      >
        {icon && (
          <i
            className={`fa fa-${icon} fa-fw ${styles.icon}`}
            style={{ fontSize: '30px', verticalAlign: 'middle' }}
          />
        )}
        {children}
      </Elem>
    );
  }
}

export default Button;
