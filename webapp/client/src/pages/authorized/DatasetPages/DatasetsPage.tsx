import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import DatasetWidget from 'components/DatasetWidget/DatasetWidget';
import {
  IFilterContext,
  selectCurrentContextFilters,
} from 'core/features/filter';
import { defaultQuickFilters } from 'features/filter/Model';
import Button from 'core/shared/view/elements/Button/Button';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes, { GetRouteParams } from 'routes';
import {
  changeDatasetsPaginationWithLoading,
  getDefaultDatasetsOptions,
  loadDatasets,
  resetDatasetsPagination,
  selectCommunications,
  selectDatasets,
  selectDatasetsPagination,
} from 'store/datasets';
import { IApplicationState } from 'store/store';

import styles from './DatasetsPage.module.css';
import DeletingDatasetsManager from './DeletingDatasetsManager/DeletingDatasetsManager';
import DatasetsPagesLayout from './shared/DatasetsPagesLayout/DatasetsPagesLayout';

const mapStateToProps = (state: IApplicationState) => {
  return {
    filters: selectCurrentContextFilters(state),
    datasets: selectDatasets(state),
    loadingDatasets: selectCommunications(state).loadingDatasets,
    pagination: selectDatasetsPagination(state),
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
  ReturnType<typeof mapDispatchToProps> &
  RouteComponentProps<GetRouteParams<typeof routes.datasets>>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class DatasetsPage extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  private filterContext: IFilterContext;

  constructor(props: AllProps) {
    super(props);
    const contextName = `datasets`;
    this.filterContext = {
      quickFilters: [
        defaultQuickFilters.name,
        defaultQuickFilters.description,
        defaultQuickFilters.tag,
      ],
      name: contextName,
      onApplyFilters: filters => {
        if (this.state.isNeedResetPagination) {
          this.props.resetDatasetsPagination();
        }
        this.props.loadDatasets(filters, props.match.params.workspaceName);
        if (!this.state.isNeedResetPagination) {
          this.setState({ isNeedResetPagination: true });
        }
      },
    };
    this.props.getDefaultDatasetsOptions();
  }

  public render() {
    const {
      datasets,
      loadingDatasets,
      filters,
      pagination,
      match,
    } = this.props;
    return (
      <DatasetsPagesLayout
        filterBarSettings={{
          context: this.filterContext,
          placeholderText: 'Drag and drop tags here',
        }}
      >
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
                return filters.length > 0 || pagination.currentPage !== 0 ? (
                  <NoResultsStub />
                ) : (
                  <NoEntitiesStub entitiesText="datasets" />
                );
              }
              return (
                <div className={styles.content}>
                  <div className={styles.deleting_datasets_manager}>
                    <DeletingDatasetsManager
                      workspaceName={match.params.workspaceName}
                    />
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
      </DatasetsPagesLayout>
    );
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.changeDatasetsPaginationWithLoading(
      currentPage,
      this.props.filters,
      this.props.match.params.workspaceName
    );
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DatasetsPage);
