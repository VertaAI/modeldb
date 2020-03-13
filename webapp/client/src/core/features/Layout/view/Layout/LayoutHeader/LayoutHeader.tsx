import * as React from 'react';
import { Link } from 'react-router-dom';

import logo from './images/Verta_logo.svg';
import styles from './LayoutHeader.module.css';

interface ILocalProps {
  rightContent: React.ReactNode;
}

class LayoutHeader extends React.PureComponent<ILocalProps> {
  public render() {
    const { rightContent } = this.props;
    return (
      <header className={styles.header}>
        <div className={styles.logo}>
          <Link to={'/'}>
            <img src={logo} />
          </Link>
        </div>
        {rightContent && (
          <div className={styles.rightContent}>{rightContent}</div>
        )}
      </header>
    );
  }
}

export default LayoutHeader;
