import cn from 'classnames';
import * as React from 'react';

import { NavLink } from 'react-router-dom';

import Preloader from '../Preloader/Preloader';
import styles from './Fab.module.css';

interface ILocalProps {
  children: React.ReactNode;
  theme: 'blue' | 'green' | 'red' | 'gray' | 'white';
  disabled?: boolean;
  isLoading?: boolean;
  variant?: 'outlined' | 'default';
  icon?: React.ReactNode;
  size?: 'medium' | 'default' | 'dynamic';
  dataTest?: string;
  to?: string;
  onClick?(): void;
}

// float action button
class Fab extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      icon,
      theme,
      variant = 'default',
      isLoading = false,
      disabled = false,
      size,
      dataTest,
      to,
      onClick,
    } = this.props;
    const Elem = (props: any) =>
      to ? (
        <NavLink to={to} data-test={dataTest} {...props} />
      ) : (
        <button data-test={dataTest} {...props} type="button" />
      );
    return (
      <Elem
        className={cn(styles.fab, {
          [styles.loading]: isLoading,
          [styles.variant_outlined]: variant === 'outlined',
          [styles.variant_default]: variant === 'default',
          [styles.theme_blue]: theme === 'blue',
          [styles.theme_green]: theme === 'green',
          [styles.theme_red]: theme === 'red',
          [styles.theme_gray]: theme === 'gray',
          [styles.theme_white]: theme === 'white',
          [styles.fab_medium]: size === 'medium',
          [styles.fab_dynamic]: size === 'dynamic',
          [styles.disabled]: disabled,
        })}
        disabled={disabled}
        onClick={onClick}
      >
        {isLoading ? (
          <div className={styles.preloader}>
            <Preloader variant="circle" dynamicSize={true} theme="light" />
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

export default Fab;
