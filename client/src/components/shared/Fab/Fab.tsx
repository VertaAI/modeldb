import cn from 'classnames';
import * as React from 'react';

import Preloader from '../Preloader/Preloader';
import styles from './Fab.module.css';

interface ILocalProps {
  children: React.ReactNode;
  theme: 'blue' | 'green' | 'red';
  disabled?: boolean;
  isLoading?: boolean;
  variant?: 'outlined' | 'default';
  icon?: React.ReactNode;
  onClick(): void;
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
          [styles.theme_red]: theme === 'red',
        })}
        disabled={disabled}
        onClick={onClick}
      >
        {!isLoading && (
          <>
            {children}
            {icon && <div className={styles.icon}>{icon}</div>}
          </>
        )}
        <div className={styles.loader}>
          <Preloader variant="circle" dynamicSize={true} />
        </div>
      </button>
    );
  }
}

export default Fab;
