import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  IColumnConfig,
  selectColumnConfig,
} from 'core/features/experimentRunsTableConfig';
import { selectCurrentContextAppliedFilters } from 'core/features/filter';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import { ICommunication } from 'core/shared/utils/redux/communication';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import ModelRecord from 'models/ModelRecord';
import {
  selectExperimentRuns,
  selectExperimentRunsPagination,
  selectExperimentRunsSorting,
  changePaginationWithLoadingExperimentRuns,
  changeSortingWithLoadingExperimentRuns,
  selectLoadingExperimentRuns,
} from 'store/experimentRuns';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

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
            <>
              <div className={styles.actions}>
                <DashboardActions
                  projectId={projectId}
                  isEnableBulkDeletionMenuToggler={data.length > 0}
                  onToggleShowingBulkDeletion={
                    this.toggleShowingBulkDeletionMenu
                  }
                />
              </div>
              <div className={styles.table}>
                <Table
                  withBulkDeletion={data.length > 0}
                  isShowBulkDeleteMenu={isShowBulkDeleteMenu}
                  projectId={projectId}
                  columnConfig={columnConfig}
                  pagination={pagination}
                  data={data}
                  sorting={sorting}
                  onSortingChange={this.onSortingChange}
                  onCurrentPageChange={this.onPaginationCurrentPageChange}
                  resetShowingBulkDeletionMenu={
                    this.resetShowingBulkDeletionMenu
                  }
                />
              </div>
            </>
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
