import moment from 'moment';
import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from 'core/shared/routes';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IHydratedCommit } from 'core/shared/models/Versioning/RepositoryData';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';

import ShortenedSHA from '../../../../../../../../shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import styles from './CurrentCommitInfo.module.css';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: IHydratedCommit;
}

const CurrentCommitInfo = (props: ILocalProps) => {
  const {
    repositoryName,
    data: { author, message, sha, dateCreated },
  } = props;
  return (
    <div className={styles.root}>
      <div className={styles.row}>
        <span className={styles.author__text}>Last change done by</span>
      </div>
      <div className={styles.row}>
        <div className={styles.author}>
          <Avatar
            username={author.username}
            sizeInPx={28}
            picture={author.picture}
          />
          <div className={styles.author__username}>{author.username}</div>
        </div>
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
