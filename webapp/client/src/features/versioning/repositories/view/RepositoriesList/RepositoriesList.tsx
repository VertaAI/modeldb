import React from 'react';
import { connect } from 'react-redux';

import DefaultMatchRemoteData from 'shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import NoEntitiesStub from 'shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import Pagination from 'shared/view/elements/Pagination/Pagination';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspace } from 'features/workspaces/store';

import RepositoryWidget from '../RepositoryWidget/RepositoryWidget';
import styles from './RepositoriesList.module.css';
import { useRepositoriesQuery } from '../../store/repositoriesQuery/repositoriesQuery';
import Reloading from 'shared/view/elements/Reloading/Reloading';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspace: selectCurrentWorkspace(state),
});

type AllProps = ReturnType<typeof mapStateToProps>;

const RepositoriesList: React.FC<AllProps> = ({ currentWorkspace }) => {
  const {
    communication: loadingRepositories,
    data: repositoriesWithPagination,
    refetch,
    onChangeCurrentPage,
  } = useRepositoriesQuery({
    workspace: currentWorkspace,
  });

  return (
    <Reloading onReload={refetch}>
      <DefaultMatchRemoteData
        communication={loadingRepositories}
        data={repositoriesWithPagination}
      >
        {({ repositories, pagination }) =>
          repositories.length > 0 ? (
            <div>
              {repositories.map(repository => (
                <RepositoryWidget
                  key={repository.id}
                  repository={repository}
                  onDeleted={() => refetch()}
                />
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
    </Reloading>
  );
};

export default connect(mapStateToProps)(React.memo(RepositoriesList));
