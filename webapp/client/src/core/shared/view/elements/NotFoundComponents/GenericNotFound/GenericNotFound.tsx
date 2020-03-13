import * as React from 'react';
import { Link } from 'react-router-dom';

import { defaultErrorMessages } from 'core/shared/utils/customErrorMessages';

import NotFoundInfographic from '../images/NotFoundInfographic';
import styles from './GenericNotFound.module.css';

export const GenericNotFound = () => {
  return (
    <div className={styles.content}>
      <div className={styles.header}>{defaultErrorMessages.page_error}</div>
      <div className={styles.links}>
        Visit the <Link to={'/'}>Homepage</Link> or contact us about the
        problem.
      </div>
      <div className={styles.infographicContainer}>
        <NotFoundInfographic
          errorCode={defaultErrorMessages.default_error_code}
        />
      </div>
    </div>
  );
};
