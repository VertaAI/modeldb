import React from 'react';
import { Omit, connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import { IFilterContext } from 'core/features/filter';
import { defaultQuickFilters } from 'features/filter/Model';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import {
  loadExperimentRuns,
  resetPagination,
  getExperimentRunsOptions,
  lazyLoadChartData,
  selectSequentialChartData,
} from 'store/experimentRuns';
import { IConnectedReduxProps, IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import makeExprRunsFilterContextName from '../makeExprRunsFilterContextName';
import ProjectPageTabs, {
  IProjectPageTabsLocalProps,
} from '../ProjectPageTabs/ProjectPageTabs';

import styles from './LayoutWithExprRunsFilter.module.css';

interface IPropsFromState {
  sequentialChartData: ModelRecord[] | null;
}

interface ILocalProps {
  children: Exclude<React.ReactNode, null | undefined>;
  tabsSetting?: Omit<IProjectPageTabsLocalProps, 'projectId'>;
}

type IUrlProps = GetRouteParams<typeof routes.experimentRuns>;
type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  ILocalProps &
  IConnectedReduxProps;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class ProjectDetailsPage extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  private filterContext: IFilterContext;

  constructor(props: AllProps) {
    super(props);
    const projectId = props.match.params.projectId;
    this.filterContext = {
      quickFilters: [defaultQuickFilters.name, defaultQuickFilters.tag],
      name: makeExprRunsFilterContextName(projectId),
      onApplyFilters: (filters, dispatch) => {
        const isChartsPage = Boolean(
          routes.charts.getMatch(window.location.pathname)
        );
        if (
          Boolean(routes.experimentRuns.getMatch(window.location.pathname)) &&
          this.state.isNeedResetPagination
        ) {
          dispatch(resetPagination(projectId));
        }

        if (isChartsPage) {
          if (this.props.sequentialChartData) {
            if (this.props.sequentialChartData.length === 0) {
              dispatch(lazyLoadChartData(projectId, filters));
            }
          } else {
            dispatch(lazyLoadChartData(projectId, filters));
          }
        } else {
          dispatch(loadExperimentRuns(projectId, filters));
        }

        if (!this.state.isNeedResetPagination) {
          this.setState({ isNeedResetPagination: true });
        }
      },
    };
    this.props.dispatch(getExperimentRunsOptions(projectId));
  }

  public render() {
    return (
      <ProjectsPagesLayout
        filterBarSettings={{
          placeholderText: 'Drag and drop parameters and tags here',
          context: this.filterContext,
          withFilterIdsSection: true,
        }}
      >
        <div className={styles.root}>
          <ProjectPageTabs
            {...this.props.tabsSetting || {}}
            projectId={this.props.match.params.projectId}
          />
          <div className={styles.content}>{this.props.children}</div>
        </div>
      </ProjectsPagesLayout>
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  sequentialChartData: selectSequentialChartData(state),
});

export default withRouter(connect(mapStateToProps)(ProjectDetailsPage));
