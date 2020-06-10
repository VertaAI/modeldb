import * as React from 'react';

import { SearchResults } from 'core/features/highLevelSearch';

import { AuthorizedLayout } from '../shared/AuthorizedLayout';
import styles from './HighLevelSearchPage.module.css';

const HighLevelSearchPage = () => {
  return (
    <AuthorizedLayout>
      <div className={styles.root}>
        <SearchResults />
      </div>
    </AuthorizedLayout>
  );
};

export default HighLevelSearchPage;
