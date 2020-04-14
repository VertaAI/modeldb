import moment from 'moment';
import * as React from 'react';
import { useSelector } from 'react-redux';
import { NavLink } from 'react-router-dom';
import cn from 'classnames';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import ShortenedSHA from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import routes from 'routes';
import { selectCurrentWorkspaceName } from 'store/workspaces';
import Tooltip from 'core/shared/view/elements/Tooltip/Tooltip';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';

import styles from './Commit.module.css';
import copy from 'copy-to-clipboard';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: IHydratedCommit;
}

const Commit = (props: ILocalProps) => {
  const { data, repositoryName } = props;
  const currentWorkspaceName = useSelector(selectCurrentWorkspaceName);
  return (
    <div className={styles.root}>
      <div className={styles.message} title={data.message}>
        {data.message}
      </div>
      <div className={styles.info}>
        <div className={styles.meta}>
          <div className={styles.author}>
            <Avatar
              sizeInPx={28}
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
        <div className={styles.actions}>
          <div className={styles.commitShaWithCopy}>
            <Tooltip content="Commit SHA">
              <NavLink
                className={cn(styles.commitShaWithCopy__item, {
                  [styles.commitSha]: true,
                })}
                to={routes.repositoryCommit.getRedirectPathWithCurrentWorkspace(
                  {
                    repositoryName,
                    commitSha: data.sha,
                  }
                )}
              >
                <ShortenedSHA sha={data.sha} />
              </NavLink>
            </Tooltip>
            <Tooltip content="Copy">
              <div className={styles.commitShaWithCopy__item} onClick={() => copy(data.sha)}>
                <Icon type="copy-to-clipboard" className={styles.icon} />
              </div>
            </Tooltip>
          </div>
          <Tooltip content="Repository">
            <NavLink
              to={routes.repositoryDataWithLocation.getRedirectPath({
                workspaceName: currentWorkspaceName,
                repositoryName,
                type: 'folder',
                commitPointer: CommitPointerHelpers.makeFromCommitSha(data.sha),
                location: CommitComponentLocation.makeRoot(),
              })}
              className={styles.viewCommitComponents}
            >
              <Icon type="repository" className={styles.icon} />
            </NavLink>
          </Tooltip>
        </div>
      </div>
    </div>
  );
};

export default Commit;
