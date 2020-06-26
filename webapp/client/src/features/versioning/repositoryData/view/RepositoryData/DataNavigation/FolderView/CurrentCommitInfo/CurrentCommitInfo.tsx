import moment from 'moment';
import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from 'shared/routes';

import { IRepository } from 'shared/models/Versioning/Repository';
import { IHydratedCommit } from 'shared/models/Versioning/RepositoryData';

import ShortenedSHA from '../../../../../../../../shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import styles from './CurrentCommitInfo.module.css';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: IHydratedCommit;
}

const CurrentCommitInfo = (props: ILocalProps) => {
  const {
    repositoryName,
    data: { message, sha, dateCreated },
  } = props;
  return (
    <div className={styles.root}>
      <div className={styles.row}>
        <span className={styles.author__text}></span>
      </div>
      <div className={styles.row}>
        <div className={styles.author}></div>
        <div className={styles.message} title={message}>
          {message}
        </div>
        <div className={styles.dateCreated}>
          Latest commit{' '}
          <Link
            className={styles.commitSha}
            to={routes.repositoryCommit.getRedirectPathWithCurrentWorkspace({
              commitSha: sha,
              repositoryName,
            })}
          >
            <ShortenedSHA sha={sha} />
          </Link>{' '}
          {moment(dateCreated).fromNow()}
        </div>
      </div>
    </div>
  );
};

export default CurrentCommitInfo;
