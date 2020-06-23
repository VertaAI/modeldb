import React from 'react';
import { useHistory } from 'react-router';
import { useSelector } from 'react-redux';
import { Icon } from 'shared/view/elements/Icon/Icon';

import { selectors } from '../../store';
import styles from './RepositoryNavigation.module.css';

const RepositoryNavigation = () => {
  const history = useHistory();

  const isBackEnabled = useSelector(selectors.isBackEnabled);
  const isForwardEnabled = useSelector(selectors.isForwardEnabled);

  return (
    <div className={styles.root}>
      <button
        className={styles.button}
        disabled={!isBackEnabled}
        onClick={() => {
          history.goBack();
        }}
      >
        <Icon type="arrow-left" className={styles.button__icon} />
      </button>
      <button
        className={styles.button}
        disabled={!isForwardEnabled}
        onClick={() => {
          history.goForward();
        }}
      >
        <Icon type="arrow-right" className={styles.button__icon} />
      </button>
    </div>
  );
};

export default RepositoryNavigation;
