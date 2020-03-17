import React, { useCallback } from 'react';
import { NavLink } from 'react-router-dom';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import routes from 'routes';

import RepositoryLabelsManager from './RepositoryLabelsManager/RepositoryLabelsManager';
import styles from './RepositoryWidget.module.css';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';

interface ILocalProps {
  repository: IRepository;
}

const RepositoryWidget: React.FC<ILocalProps> = ({ repository }) => {
  const preventRedirect = useCallback(
    (e: React.MouseEvent) => e.preventDefault(),
    []
  );

  return (
    <NavLink
      className={styles.root}
      to={routes.repositoryData.getRedirectPathWithCurrentWorkspace({
        repositoryName: repository.name,
      })}
    >
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
    </NavLink>
  );
};

export default React.memo(RepositoryWidget);
