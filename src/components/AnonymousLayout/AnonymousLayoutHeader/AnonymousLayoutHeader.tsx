import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './AnonymousLayoutHeader.module.css';

export default class AnonymousLayoutHeader extends React.PureComponent {
  public render() {
    return (
      <header className={styles.header}>
        <nav className={styles.nav_menu}>
          <Link to={'/'}>About</Link>
          <Link to={'/'}>Blog</Link>
          <Link to={'/'}>Careers</Link>
        </nav>
      </header>
    );
  }
}
