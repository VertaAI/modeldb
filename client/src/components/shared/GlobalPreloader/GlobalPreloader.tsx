import * as React from 'react';

import loader from 'components/images/loader.gif';
import Preloader from '../Preloader/Preloader';

import styles from './GlobalPreloader.module.css';

const GlobalPreloader = React.memo(() => {
  return (
    <div className={styles.global_preloader}>
      <Preloader variant="dots" />
    </div>
  );
});

export default GlobalPreloader;
