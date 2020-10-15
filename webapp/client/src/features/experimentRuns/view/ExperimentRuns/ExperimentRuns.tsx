import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import {
  IColumnConfig,
  selectColumnConfig,
} from 'features/experimentRunsTableConfig';
import { selectCurrentContextAppliedFilters } from 'features/filter';
import { IFilterData } from 'shared/models/Filters';
import { IPagination } from 'shared/models/Pagination';
import { ISorting } from 'shared/models/Sorting';
import { ICommunication } from 'shared/utils/redux/communication';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import ModelRecord from 'shared/models/ModelRecord';
import {
  selectExperimentRuns,
  selectExperimentRunsPagination,
  selectExperimentRunsSorting,
  changePaginationWithLoadingExperimentRuns,
  changeSortingWithLoadingExperimentRuns,
  selectLoadingExperimentRuns,
} from 'features/experimentRuns/store';
import { IApplicationState, IConnectedReduxProps } from 'setup/store/store';

import DashboardActions from './DashboardActions/DashboardActions';
import styles from './ExperimentRuns.module.css';
import Table from './Table/Table';

interface ILocalProps {
  projectId: string;
}

interface IPropsFromState {
  data: ModelRecord[] | null;
  loading: ICommunication;
  columnConfig: IColumnConfig;
  pagination: IPagination;
  filters: IFilterData[];
  sorting: ISorting | null;
}

export type AllProps = IPropsFromState & IConnectedReduxProps & ILocalProps;

export interface ILocalState {
  isShowBulkDeleteMenu: boolean;
}

class ExperimentRuns extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isShowBulkDeleteMenu: false,
  };

  public render() {
    const {
      data,
      loading,
      columnConfig,
      pagination,
      sorting,
      projectId,
    } = this.props;
    const { isShowBulkDeleteMenu } = this.state;

    return (
      <div className={styles.root}>
        {(() => {
          if (loading.isRequesting) {
            return (
              <div className={styles.preloader}>
                <Preloader variant="dots" />
              </div>
            );
          }
          if (loading.error || !data) {
            return (
              <PageCommunicationError
                error={loading.error}
                isNillEntity={!data}
              />
            );
          }
          return (
            // todo find the better way
            <WithCurrentUserActionsAccesses
              entityType={data.length > 0 ? 'experimentRun' : 'project'}
              entityId={data.length > 0 ? data[0].id : projectId}
              actions={['delete']}
            >
              {({ actionsAccesses }) => (
                <>
                  <div className={styles.actions}>
                    <DashboardActions
                      projectId={projectId}
                      isEnableBulkDeletionMenuToggler={
                        actionsAccesses.delete && data.length > 0
                      }
                      onToggleShowingBulkDeletion={
                        this.toggleShowingBulkDeletionMenu
                      }
                    />
                  </div>
                  <div className={styles.table}>
                    <Table
                      withBulkDeletion={
                        actionsAccesses.delete && data.length > 0
                      }
                      isShowBulkDeleteMenu={isShowBulkDeleteMenu}
                      projectId={projectId}
                      columnConfig={columnConfig}
                      pagination={pagination}
                      data={data}
                      sorting={sorting}
                      onSortingChange={this.onSortingChange}
                      onCurrentPageChange={
                        this.onPaginationCurrentPageChange
                      }
                      onResetShowingBulkDeletionMenu={
                        this.resetShowingBulkDeletionMenu
                      }
                    />
                  </div>
                </>
              )}
            </WithCurrentUserActionsAccesses>
          );
        })()}
      </div>
    );
  }

  @bind
  private toggleShowingBulkDeletionMenu() {
    this.setState(prev => ({
      isShowBulkDeleteMenu: !prev.isShowBulkDeleteMenu,
    }));
  }

  @bind
  private resetShowingBulkDeletionMenu() {
    this.setState({
      isShowBulkDeleteMenu: false,
    });
  }

  @bind
  private onSortingChange(sorting: ISorting | null) {
    this.props.dispatch(
      changeSortingWithLoadingExperimentRuns(
        this.props.projectId,
        sorting,
        this.props.filters
      )
    );
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.dispatch(
      changePaginationWithLoadingExperimentRuns(
        this.props.projectId,
        currentPage,
        this.props.filters
      )
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  columnConfig: selectColumnConfig(state),
  data: selectExperimentRuns(state),
  loading: selectLoadingExperimentRuns(state),
  pagination: selectExperimentRunsPagination(state),
  filters: selectCurrentContextAppliedFilters(state),
  sorting: selectExperimentRunsSorting(state),
});

export default connect(mapStateToProps)(ExperimentRuns);
