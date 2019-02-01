import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './GenericNotFound.module.css';
import notFoundImg from './images/404img.svg';

// tslint:disable-next-line:variable-name
export const GenericNotFound = () => {
  return (
    <div className={styles.content}>
      <div className={styles.header}>Page not found.</div>
      <div className={styles.links}>
        Visit the <Link to={'/'}>Homepage</Link> or <Link to={'/'}>contact us</Link> about the problem
      </div>
      <div className={styles.picture}>
        <img src={notFoundImg} />
      </div>
    </div>
  );
};
