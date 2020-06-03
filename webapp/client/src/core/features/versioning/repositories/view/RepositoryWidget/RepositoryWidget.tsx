import React, { useCallback } from 'react';
import cn from 'classnames';
import { NavLink } from 'react-router-dom';

import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import routes from 'routes';

import RepositoryLabelsManager from './RepositoryLabelsManager/RepositoryLabelsManager';
import styles from './RepositoryWidget.module.css';
import useDeleteRepository from '../useDeleteRepository';

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps;

const RepositoryWidget: React.FC<AllProps> = ({ repository }) => {
  const preventRedirect = useCallback(
    (e: React.MouseEvent) => e.preventDefault(),
    []
  );

  const { deleteRepositoryButton, isDeletingRepository } = useDeleteRepository({
    repository,
  });

  return (
    <NavLink
      className={cn(styles.root, {
        [styles.deleting]: isDeletingRepository,
      })}
      to={routes.repositoryData.getRedirectPathWithCurrentWorkspace({
        repositoryName: repository.name,
      })}
    >
      <div className={styles.info}>
        <div className={styles.left}>
          <span className={styles.name}>{repository.name}</span>
          <div onClick={preventRedirect}>
            <RepositoryLabelsManager repository={repository} />
          </div>
        </div>
        <div className={styles.right}>
          <div className={styles.owner_block}>
            <div className={styles.owner_username}>
              <div>{repository.owner.username}</div>
              <div className={styles.owner_status}>Owner</div>
            </div>
            <Avatar
              username={repository.owner.username}
              sizeInPx={36}
              picture={repository.owner.picture}
            />
          </div>
          <span
            className={styles.date}
          >{`Created: ${repository.dateCreated.toLocaleDateString()}`}</span>
          <span
            className={styles.date}
          >{`Updated: ${repository.dateUpdated.toLocaleDateString()}`}</span>
        </div>
      </div>
      <div className={styles.actions}>
        {deleteRepositoryButton && (
          <>
            <div
              className={cn(styles.action, { [styles.action_delete]: true })}
              onClick={preventRedirect}
            >
              {deleteRepositoryButton}
            </div>
          </>
        )}
      </div>
    </NavLink>
  );
};

export default React.memo(RepositoryWidget);
