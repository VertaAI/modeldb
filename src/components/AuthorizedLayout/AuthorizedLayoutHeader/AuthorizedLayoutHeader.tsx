import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from '../../../routes';
import styles from './AuthorizedLayoutHeader.module.css';
import Breadcrumb from './Breadcrumb/Breadcrumb';
import logo from './images/Verta logo.svg';
import UserBar from './UserBar/UserBar';

export default class AuthorizedLayoutHeader extends React.PureComponent {
  public render() {
    return (
      <header className={styles.header}>
        <div className={styles.logo}>
          <Link to={routes.mainPage.getPath()}>
            <img src={logo} />
          </Link>
        </div>
        <div className={styles.header_content}>
          <div className={styles.breadcrumb}>
            <Breadcrumb />
          </div>
          <nav className={styles.nav_menu}>
            <Link to={routes.mainPage.getPath()}>About</Link>
            <Link to={routes.mainPage.getPath()}>Blog</Link>
          </nav>
          <div className={styles.user_bar}>
            <UserBar />
          </div>
        </div>
      </header>
    );
  }
}
