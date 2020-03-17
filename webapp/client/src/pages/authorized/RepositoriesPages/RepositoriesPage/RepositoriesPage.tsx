import React from 'react';
import { connect } from 'react-redux';

import { RepositoriesList } from 'core/features/repository/repositories/view';
import Button from 'core/shared/view/elements/Button/Button';
import routes from 'routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'store/workspaces';

import RepositoriesPagesLayout from '../shared/RepositoriesPagesLayout/RepositoriesPagesLayout';
import styles from './RepositoriesPage.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspace: selectCurrentWorkspace(state),
});

type AllProps = ReturnType<typeof mapStateToProps>;

const RepositoriesPage: React.FC<AllProps> = ({ currentWorkspace }) => {
  return (
    <RepositoriesPagesLayout>
      <div className={styles.root}>
        <div className={styles.header}>
          <Button
            to={routes.createRepository.getRedirectPath({
              workspaceName: currentWorkspace.name,
            })}
          >
            Create
          </Button>
        </div>
        <div className={styles.list}>
          <RepositoriesList />
        </div>
      </div>
    </RepositoriesPagesLayout>
  );
};

export default connect(mapStateToProps)(React.memo(RepositoriesPage));
