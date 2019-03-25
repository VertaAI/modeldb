import * as React from 'react';
import { bind } from 'decko';

import uploadSrc from './imgs/upload.svg';
import styles from './Fab.module.css';

interface IProps {
  children: React.ReactNode;
  icon?: Icon;
  theme?: 'blue';
  onClick(): void;
}

type Icon = 'upload';

// float action button
class Fab extends React.Component<IProps> {
  public render() {
    const { children, icon, onClick } = this.props;
    return (
      <button className={styles.fab} onClick={onClick}>
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
