import React, { useEffect, useCallback } from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';
import { Dispatch, bindActionCreators } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import routes from 'routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'store/workspaces';

import { actions } from '../../store';
import {
  selectRepositories,
  selectCommunications,
  selectPagination,
} from '../../store/selectors';
import RepositoryWidget from '../RepositoryWidget/RepositoryWidget';
import styles from './RepositoriesList.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspace: selectCurrentWorkspace(state),
  repositories: selectRepositories(state),
  loadingRepositories: selectCommunications(state).loadingRepositories,
  pagination: selectPagination(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadRepositoriesWithURLPagination:
        actions.loadRepositoriesWithURLPagination,
      resetLoadingRepositories: actions.loadRepositories.reset,
      changePageWithLoadRepositories: actions.changePageWithLoadRepositories,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const RepositoriesList: React.FC<AllProps> = ({
  loadRepositoriesWithURLPagination,
  currentWorkspace,
  loadingRepositories,
  resetLoadingRepositories,
  repositories,
  pagination,
  changePageWithLoadRepositories,
}) => {
  useEffect(() => {
    loadRepositoriesWithURLPagination({
      workspaceName: currentWorkspace.name,
    });
  }, []);

  useEffect(() => {
    return () => {
      resetLoadingRepositories();
    };
  }, []);

  const history = useHistory();

  const onChangeCurrentPage = useCallback((page: number) => {
    if (page !== 0) {
      history.push(
        routes.repositories.getRedirectPathWithQueryParams({
          params: { workspaceName: currentWorkspace.name },
          queryParams: { page: String(page + 1) },
        })
      );
    } else {
      history.push(
        routes.repositories.getRedirectPath({
          workspaceName: currentWorkspace.name,
        })
      );
    }

    changePageWithLoadRepositories({
      page,
      workspaceName: currentWorkspace.name,
    });
  }, []);

  return (
    <DefaultMatchRemoteData
      communication={loadingRepositories}
      data={repositories}
    >
      {loadedRepositories =>
        loadedRepositories.length > 0 ? (
          <div>
            {loadedRepositories.map(repository => (
              <RepositoryWidget key={repository.id} repository={repository} />
            ))}
            <div className={styles.pagination}>
              <Pagination
                pagination={pagination}
                onCurrentPageChange={onChangeCurrentPage}
              />
            </div>
          </div>
        ) : (
          <NoEntitiesStub entitiesText="Repository" />
        )
      }
    </DefaultMatchRemoteData>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(React.memo(RepositoriesList));
