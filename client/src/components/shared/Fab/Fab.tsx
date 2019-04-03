import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import styles from './Fab.module.css';
import loaderSrc from './imgs/preloader.svg';
import uploadSrc from './imgs/upload.svg';

interface ILocalProps {
  children: React.ReactNode;
  theme: 'blue' | 'green';
  disabled?: boolean;
  isLoading?: boolean;
  variant?: 'outlined' | 'default';
  icon?: Icon;
  onClick(): void;
}

type Icon = 'upload';

// float action button
class Fab extends React.Component<ILocalProps> {
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
            {icon && <img className={styles.icon} src={this.getIconSrc()} />}
          </>
        )}
        <img className={styles.loader} src={loaderSrc} />
      </button>
    );
  }

  @bind
  private getIconSrc() {
    return ({ upload: uploadSrc } as Record<Icon, string>)[this.props.icon!];
  }
}

export default Fab;
