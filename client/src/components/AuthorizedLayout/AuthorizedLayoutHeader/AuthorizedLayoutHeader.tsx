import * as React from 'react';
import { Link } from 'react-router-dom';

import styles from './AuthorizedLayoutHeader.module.css';
import Breadcrumb from './Breadcrumb/Breadcrumb';
import logo from './images/Verta_logo.svg';
import UserBar from './UserBar/UserBar';

export default class AuthorizedLayoutHeader extends React.PureComponent {
  public render() {
    return (
      <header className={styles.header}>
        <div className={styles.logo}>
          <Link to={'/'}>
            <img src={logo} />
          </Link>
        </div>
        <div className={styles.header_content}>
          <div className={styles.breadcrumb}>
            <Breadcrumb />
          </div>
          <nav className={styles.nav_menu}>
            <Link to={'/'}>About</Link>
            <Link to={'/'}>Blog</Link>
          </nav>
          <div className={styles.user_bar}>
            <UserBar />
          </div>
        </div>
      </header>
    );
  }
}
