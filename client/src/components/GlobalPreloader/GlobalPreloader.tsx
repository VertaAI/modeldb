import * as React from 'react';
import styles from './GlobalPreloader.module.css';
import loader from '../images/loader.gif';

const GlobalPreloader = React.memo(() => {
  return (
    <div className={styles.global_preloader}>
      <img src={loader} />
    </div>
  );
});

export default GlobalPreloader;
