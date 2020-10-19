import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import Button from 'shared/view/elements/Button/Button';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'shared/view/elements/Pagination/Pagination';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import Reloading from 'shared/view/elements/Reloading/Reloading';
import {
  changeDatasetsPaginationWithLoading,
  getDefaultDatasetsOptions,
  loadDatasets,
  resetDatasetsPagination,
  selectCommunications,
  selectDatasets,
  selectDatasetsPagination,
} from 'features/datasets/store';
import routes from 'shared/routes';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import styles from './Datasets.module.css';
import DatasetWidget from './DatasetWidget/DatasetWidget';
import DeletingDatasetsManager from './DeletingDatasetsManager/DeletingDatasetsManager';

const mapStateToProps = (state: IApplicationState) => {
  return {
    datasets: selectDatasets(state),
    loadingDatasets: selectCommunications(state).loadingDatasets,
    pagination: selectDatasetsPagination(state),
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadDatasets,
      resetDatasetsPagination,
      changeDatasetsPaginationWithLoading,
      getDefaultDatasetsOptions,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class Datasets extends React.PureComponent<AllProps, ILocalState> {
  public componentDidMount() {
    this.loadDatasets();
  }

  public render() {
    const { datasets, loadingDatasets, pagination, workspaceName } = this.props;

    return (
      <Reloading onReload={this.loadDatasets}>
        <div className={styles.root}>
          <div className={styles.actions}>
            <div className={styles.action}>
              <Button
                to={routes.datasetCreation.getRedirectPathWithCurrentWorkspace(
                  {}
                )}
              >
                Create
              </Button>
            </div>
          </div>
          <div className={styles.root} data-type="datasets-page">
            {(() => {
              if (loadingDatasets.isRequesting) {
                return <Preloader variant="dots" />;
              }
              if (loadingDatasets.error || !datasets) {
                return (
                  <PageCommunicationError
                    error={loadingDatasets.error}
                    isNillEntity={!datasets}
                  />
                );
              }
              if (datasets.length === 0) {
                return pagination.currentPage !== 0 ? (
                  <NoResultsStub />
                ) : (
                  <NoEntitiesStub entitiesText="datasets" />
                );
              }
              return (
                <div className={styles.content}>
                  <div className={styles.deleting_datasets_manager}>
                    <DeletingDatasetsManager workspaceName={workspaceName} />
                  </div>
                  <div className={styles.datasets} data-test="datasets">
                    {datasets.map(dataset => (
                      <div className={styles.dataset} key={dataset.id}>
                        <DatasetWidget dataset={dataset} />
                      </div>
                    ))}
                  </div>
                  {datasets.length > 0 && (
                    <div className={styles.pagination}>
                      <Pagination
                        onCurrentPageChange={this.onPaginationCurrentPageChange}
                        pagination={pagination}
                      />
                    </div>
                  )}
                </div>
              );
            })()}
          </div>
        </div>
      </Reloading>
    );
  }

  @bind
  private loadDatasets() {
    this.props.loadDatasets([], this.props.workspaceName);
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.changeDatasetsPaginationWithLoading(
      currentPage,
      [],
      this.props.workspaceName
    );
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Datasets);
