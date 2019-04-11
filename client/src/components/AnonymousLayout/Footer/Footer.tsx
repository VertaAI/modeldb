import * as React from 'react';
import { Link } from 'react-router-dom';

import Icon from 'components/shared/Icon/Icon';

import styles from './Footer.module.css';

// tslint:disable-next-line:variable-name
export const Footer = () => {
  return (
    <footer className={styles.container}>
      <div className={styles.footer_site_map}>
        <span>© 2019 Verta</span>
        <Link to={'/'}>Terms</Link>
        <Link to={'/'}>Privacy</Link>
      </div>
      <div className={styles.social_media}>
        <Link className={styles.social} to={'/'}>
          <Icon type="twitter" />
        </Link>
        <Link className={styles.social} to={'/'}>
          <Icon type="facebook" />
        </Link>
        <Link className={styles.social} to={'/'}>
          <Icon type="linkedIn" />
        </Link>
        <Link className={styles.social} to={'/'}>
          <Icon type="github" />
        </Link>
      </div>
    </footer>
  );
};
