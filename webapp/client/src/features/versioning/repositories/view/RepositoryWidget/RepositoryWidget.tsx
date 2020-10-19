import React, { useCallback } from 'react';
import cn from 'classnames';
import { NavLink } from 'react-router-dom';

import { IRepository } from 'shared/models/Versioning/Repository';
import Tooltip from 'shared/view/elements/Tooltip/Tooltip';
import ActionIcon from 'shared/view/elements/ActionIcon/ActionIcon';
import routes from 'shared/routes';

import RepositoryLabelsManager from './RepositoryLabelsManager/RepositoryLabelsManager';
import styles from './RepositoryWidget.module.css';
import { useDeleteRepositoryMutation } from '../../store/deleteRepository/deleteRepository';
import { hasAccessToAction } from 'shared/models/EntitiesActions';

interface ILocalProps {
  onDeleted: (repositoryId: IRepository['id']) => void;
  repository: IRepository;
}

type AllProps = ILocalProps;

const RepositoryWidget: React.FC<AllProps> = ({ repository, onDeleted }) => {
  const preventRedirect = useCallback(
    (e: React.MouseEvent) => e.preventDefault(),
    []
  );

  const {
    deleteRepositoryButton,
    deletingRepository,
  } = useDeleteRepositoryMutation({
    repository,
    onDeleted: () => onDeleted(repository.id),
  });

  return (
    <NavLink
      className={cn(styles.root, {
        [styles.deleting]: deletingRepository.isRequesting,
      })}
      to={routes.repositoryData.getRedirectPathWithCurrentWorkspace({
        repositoryName: repository.name,
      })}
    >
      <div className={styles.info}>
        <div className={styles.left}>
          <div>
            <span className={styles.name}>{repository.name}</span>
            &nbsp;
            {!hasAccessToAction('update', repository) && (
              <Tooltip content={'Read Only Repository'}>
                <span className={styles.desc_readonly}>
                  <ActionIcon iconType="eye" />
                </span>
              </Tooltip>
            )}
          </div>
          <div className={styles.labels} onClick={preventRedirect}>
            <RepositoryLabelsManager repository={repository} />
          </div>
        </div>
        <div className={styles.right}>
          <div className={styles.owner_block}></div>
          <span
            className={styles.date}
          >{`Created: ${repository.dateCreated.toLocaleDateString()}`}</span>
          <span
            className={styles.date}
          >{`Updated: ${repository.dateUpdated.toLocaleDateString()}`}</span>
        </div>
      </div>
      <div className={styles.actions}>
        {
          <>
            {deleteRepositoryButton && (
              <div
                className={cn(styles.action, { [styles.action_delete]: true })}
                onClick={preventRedirect}
              >
                {deleteRepositoryButton}
              </div>
            )}
          </>
        }
      </div>
    </NavLink>
  );
};

export default React.memo(RepositoryWidget);
