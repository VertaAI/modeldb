import * as React from 'react';
import { Link } from 'react-router-dom';

import NotFoundInfographic from '../images/NotFoundInfographic';

import styles from './DynamicNotFound.module.css';

interface ILocalProps {
  errorCode: number | string;
  errorMessage: string;
}

class DynamicNotFound extends React.PureComponent<ILocalProps> {
  public render() {
    const { errorCode, errorMessage } = this.props;
    return (
      <div className={styles.content}>
        <div className={styles.header}>{errorMessage}</div>
        <div className={styles.links}>
          Visit the <Link to={'/'}>Homepage</Link> or contact us about the
          problem.
        </div>
        <div className={styles.infographicContainer}>
          <NotFoundInfographic errorCode={errorCode} />
        </div>
      </div>
    );
  }
}

export default DynamicNotFound;
