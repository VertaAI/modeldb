import React from 'react';

import styles from './UrlHint.module.css';

interface ILocalProps {
  label: string;
  url: string;
  dataTestUrl?: string;
}

const UrlHint: React.FC<ILocalProps> = ({ label, url, dataTestUrl }) => (
  <div className={styles.root}>
    {`${label}: `}
    <div className={styles.url} data-test={dataTestUrl}>
      {url}
    </div>
  </div>
);

export default React.memo(UrlHint);
