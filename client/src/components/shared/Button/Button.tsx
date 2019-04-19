import cn from 'classnames';
import * as React from 'react';

import styles from './Button.module.css';

interface ILocalProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  to?: string;
  fullWidth?: boolean;
  size?: 'medium' | 'large'; // todo maybe default rename to medium
  icon?: React.ReactNode;
  textTransform?: 'default' | 'none';
  theme?: 'default' | 'gray';
  onClick?(): void;
}

class Button extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      to,
      icon,
      fullWidth = false,
      size = 'medium',
      textTransform = 'default',
      theme = 'default',
      disabled,
      onClick,
    } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? <a href={to} {...props} /> : <button {...props} type="button" />;
    return (
      <Elem
        className={cn(styles.button, {
          [styles.full_width]: fullWidth,
          [styles.size_medium]: size === 'medium',
          [styles.size_large]: size === 'large',
          [styles.text_transform_default]: textTransform === 'default',
          [styles.text_transform_none]: textTransform === 'none',
          [styles.theme_default]: theme === 'default',
          [styles.theme_gray]: theme === 'gray',
        })}
        disabled={disabled}
        onClick={onClick}
      >
        {icon && <div className={styles.icon}>{icon}</div>}
        {children}
      </Elem>
    );
  }
}

export default Button;
