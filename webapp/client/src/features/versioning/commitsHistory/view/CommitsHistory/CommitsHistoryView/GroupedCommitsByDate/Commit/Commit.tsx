import moment from 'moment';
import * as React from 'react';
import { useSelector } from 'react-redux';
import { NavLink } from 'react-router-dom';
import cn from 'classnames';
import copy from 'copy-to-clipboard';

import { IRepository } from 'shared/models/Versioning/Repository';
import { CommitPointerHelpers } from 'shared/models/Versioning/RepositoryData';
import ShortenedSHA from 'shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import { Icon } from 'shared/view/elements/Icon/Icon';
import routes from 'shared/routes';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';
import Tooltip from 'shared/view/elements/Tooltip/Tooltip';
import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { ICommitView } from 'features/versioning/commitsHistory/store/types';

import styles from './Commit.module.css';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: ICommitView;
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
              <div
                className={styles.commitShaWithCopy__item}
                onClick={() => copy(data.sha)}
              >
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
