import { ButtonGroup, Button } from '@material-ui/core';
import copy from 'copy-to-clipboard';
import moment from 'moment';
import * as React from 'react';
import { Link } from 'react-router-dom';

import routes from 'routes';
import { getRedirectPathToRepositoryDataPage } from 'core/features/versioning/repositoryData';
import ShortenedSHA from 'core/shared/view/domain/Repository/ShortenedSHA/ShortenedSHA';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './Commit.module.css';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: IHydratedCommit;
}

const Commit = (props: ILocalProps) => {
  const { data, repositoryName } = props;
  return (
    <div className={styles.root}>
      <div className={styles.info}>
        {data.message && (
          <div className={styles.message} title={data.message}>
            {data.message}
          </div>
        )}
        <div className={styles.meta}>
          <div className={styles.author}>
            <Avatar
              sizeInPx={20}
              username={data.author.username}
              picture={data.author.picture}
            />
            <div className={styles.author__username}>
              {data.author.username}
            </div>
          </div>
          <div className={styles.date}>
            committed {moment(data.dateCreated).fromNow()}
          </div>
        </div>
      </div>
      <div className={styles.actions}>
        <div className={styles.actions__group}>
          <ButtonGroup>
            <Button
              classes={{ root: styles.action }}
              onClick={() => copy(data.sha)}
            >
              <Icon type="copy-to-clipboard" className={styles.action__icon} />
            </Button>
            <Button
              classes={{ root: styles.action }}
              component={Link}
              to={routes.repositoryCommit.getRedirectPathWithCurrentWorkspace({
                repositoryName,
                commitSha: data.sha,
              })}
            >
              <ShortenedSHA sha={data.sha} />
            </Button>
          </ButtonGroup>
        </div>
        <div className={styles.actions__group}>
          <Button
            classes={{ root: styles.action }}
            color="default"
            variant="outlined"
            component={Link}
            to={getRedirectPathToRepositoryDataPage({
              repositoryName,
              type: 'folder',
              commitPointer: CommitPointerHelpers.makeFromCommitSha(data.sha),
              location: DataLocation.makeRoot(),
            })}
          >
            <Icon type="repository" className={styles.action__icon} />
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Commit;
