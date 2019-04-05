import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import Icon from '../Icon/Icon';
import styles from './Fab.module.css';
import loaderSrc from './imgs/preloader.svg';

interface ILocalProps {
  children: React.ReactNode;
  theme: 'blue' | 'green';
  disabled?: boolean;
  isLoading?: boolean;
  variant?: 'outlined' | 'default';
  icon?: IconType;
  onClick(): void;
}

type IconType = 'upload';

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
      onClick,
    } = this.props;
    return (
      <button
        className={cn(styles.fab, {
          [styles.loading]: isLoading,
          [styles.variant_outlined]: variant === 'outlined',
          [styles.variant_default]: variant === 'default',
          [styles.theme_blue]: theme === 'blue',
          [styles.theme_green]: theme === 'green',
        })}
        disabled={disabled}
        onClick={onClick}
      >
        {!isLoading && (
          <>
            {children}
            {icon && <Icon type={icon} className={styles.icon} />}
          </>
        )}
        <img className={styles.loader} src={loaderSrc} />
      </button>
    );
  }
}

export default Fab;
