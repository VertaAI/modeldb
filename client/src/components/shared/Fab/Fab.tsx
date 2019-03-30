import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import styles from './Fab.module.css';
import uploadSrc from './imgs/upload.svg';

interface ILocalProps {
  children: React.ReactNode;
  theme: 'blue' | 'green';
  variant?: 'outlined' | 'default';
  icon?: Icon;
  onClick(): void;
}

type Icon = 'upload';

// float action button
class Fab extends React.Component<ILocalProps> {
  public render() {
    const { children, icon, theme, variant = 'default', onClick } = this.props;
    return (
      <button
        className={cn(styles.fab, {
          [styles.variant_outlined]: variant === 'outlined',
          [styles.variant_default]: variant === 'default',
          [styles.theme_blue]: theme === 'blue',
          [styles.theme_green]: theme === 'green',
        })}
        onClick={onClick}
      >
        {children}
        {icon && <img className={styles.icon} src={this.getIconSrc()} />}
      </button>
    );
  }

  @bind
  private getIconSrc() {
    return ({ upload: uploadSrc } as Record<Icon, string>)[this.props.icon!];
  }
}

export default Fab;
