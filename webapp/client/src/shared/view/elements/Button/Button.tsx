import cn from 'classnames';
import * as React from 'react';
import { NavLink } from 'react-router-dom';

import matchType from 'shared/utils/matchType';

import Preloader from '../Preloader/Preloader';
import styles from './Button.module.css';

interface ILocalProps {
  id?: string;
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  isLoading?: boolean;
  to?: string;
  fullWidth?: boolean;
  fullHeight?: boolean;
  size?: 'small' | 'medium' | 'large'; // todo maybe default rename to medium
  icon?: React.ReactNode;
  theme?: 'primary' | 'secondary' | 'red' | 'tertiary' | 'green';
  variant?: 'outlined';
  isExtended?: boolean;
  dataTest?: string;
  type?: any;
  name?: string;
  onClick?(e: React.MouseEvent<HTMLButtonElement, MouseEvent>): void;
  onBlur?(): void;
}

class Button extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      to,
      id,
      icon,
      fullWidth = false,
      fullHeight = false,
      size = 'medium',
      theme = 'primary',
      variant,
      disabled,
      isLoading,
      name,
      isExtended,
      dataTest,
      onClick,
      onBlur,
    } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? (
        <NavLink to={to} data-test={dataTest} {...props as any} />
      ) : (
        <button
          data-test={dataTest}
          onBlur={onBlur}
          {...props}
          name={name}
          type={this.props.type || 'button'}
        />
      );
    return (
      <Elem
        className={cn(styles.button, {
          [styles.full_width]: fullWidth,
          [styles.full_height]: fullHeight,
          [styles.size_small]: size === 'small',
          [styles.size_medium]: size === 'medium',
          [styles.size_large]: size === 'large',
          [styles.theme_primary]: theme === 'primary',
          [styles.theme_secondary]: theme === 'secondary',
          [styles.theme_tertiary]: theme === 'tertiary',
          [styles.theme_red]: theme === 'red',
          [styles.theme_green]: theme === 'green',
          [styles.outlined]: variant === 'outlined',
          [styles.disabled]: disabled,
          [styles.loading]: isLoading,
          [styles.extended]: isExtended,
        })}
        disabled={disabled || isLoading}
        id={id}
        onClick={onClick}
      >
        {isLoading ? (
          <div className={styles.preloader}>
            <Preloader
              variant="circle"
              dynamicSize={true}
              theme={matchType(
                {
                  green: () => 'light',
                  primary: () => 'light',
                  red: () => 'light',
                  secondary: () => 'blue',
                  tertiary: () => 'light',
                },
                theme
              )}
            />
          </div>
        ) : (
          <>
            {children}
            {icon && <div className={styles.icon}>{icon}</div>}
          </>
        )}
      </Elem>
    );
  }
}

export type IButtonLocalProps = ILocalProps;
export default Button;
