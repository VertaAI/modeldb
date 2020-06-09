import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';
import routes from 'shared/routes';

import {
  selectCurrentContextAppliedFilters,
  updateContextFilters,
} from 'features/filter';
import { makeDefaultExprNameFilter, PropertyType } from 'shared/models/Filters';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'shared/view/elements/Pagination/Pagination';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import Reloading from 'shared/view/elements/Reloading/Reloading';
import {
  changeExperimentsPaginationWithLoading,
  getDefaultExperimentsOptions,
  loadExperiments,
  resetExperimentsPagination,
  selectExperiments,
  selectExperimentsPagination,
  selectLoadingExperiments,
} from 'features/experiments/store';
import Experiment from 'shared/models/Experiment';
import makeExprRunsFilterContextName from 'pages/authorized/ProjectsPages/ProjectDetailsPages/shared/makeExprRunsFilterContextName';
import { selectProject } from 'features/projects/store';
import { IApplicationState } from 'store/store';

import DeletingExperimentsManager from './DeletingExperimentsManager/DeletingExperimentsManager';
import styles from './ExperimentsList.module.css';
import ExperimentWidget from './ExperimentWidget/ExperimentWidget';

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps & RouteComponentProps
) => {
  return {
    experiments: selectExperiments(state),
    project: selectProject(state, localProps.projectId),
    loadingExperimentsCommunication: selectLoadingExperiments(state),
    pagination: selectExperimentsPagination(state),
    filters: selectCurrentContextAppliedFilters(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      loadExperiments,
      updateContextFilters,
      changeExperimentsPaginationWithLoading,
      resetExperimentsPagination,
      getDefaultExperimentsOptions,
    },
    dispatch
  );

interface ILocalProps {
  projectId: string;
}

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps &
  RouteComponentProps;

class ExperimentsList extends React.PureComponent<AllProps> {
  constructor(props: AllProps) {
    super(props);
    const { projectId } = this.props;
    this.props.getDefaultExperimentsOptions(projectId);
  }

  public componentDidMount() {
    this.loadExperiments();
  }

  public render() {
    const {
      experiments,
      loadingExperimentsCommunication,
      pagination,
      filters,
      projectId,
    } = this.props;

    return (
      <Reloading onReload={this.loadExperiments}>
        {(() => {
          if (loadingExperimentsCommunication.error) {
            return (
              <PageCommunicationError
                error={loadingExperimentsCommunication.error}
              />
            );
          }
          if (loadingExperimentsCommunication.isRequesting) {
            return (
              <div className={styles.preload}>
                <Preloader variant="dots" />
              </div>
            );
          }
          if (experiments && experiments.length !== 0) {
            return (
              <>
                <DeletingExperimentsManager projectId={projectId} />
                <div className={styles.experiments}>
                  {experiments.map(experiment => (
                    <div className={styles.experiment} key={experiment.id}>
                      <ExperimentWidget
                        onViewExprRuns={this.makeOnViewExprRuns(experiment)}
                        projectId={projectId}
                        experiment={experiment}
                      />
                    </div>
                  ))}
                  <div className={styles.pagination}>
                    <Pagination
                      onCurrentPageChange={this.onPaginationCurrentPageChange}
                      pagination={pagination}
                    />
                  </div>
                </div>
              </>
            );
          }
          if (experiments && experiments.length === 0) {
            return filters.length > 0 || pagination.currentPage !== 0 ? (
              <NoResultsStub />
            ) : (
              <NoEntitiesStub entitiesText="experiments" />
            );
          }
        })()}
      </Reloading>
    );
  }

  @bind
  private loadExperiments() {
    const projectId = this.props.projectId;
    this.props.loadExperiments(projectId, []);
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.changeExperimentsPaginationWithLoading(
      this.props.projectId,
      currentPage,
      this.props.filters
    );
  }

  @bind
  private makeOnViewExprRuns(experiment: Experiment) {
    return () => {
      const { projectId } = this.props;
      this.props.updateContextFilters(
        makeExprRunsFilterContextName(projectId),
        filters => {
          return filters
            .filter(({ type }) => type !== PropertyType.EXPERIMENT_NAME)
            .concat(makeDefaultExprNameFilter(experiment.name));
        }
      );
      this.props.history.push(
        routes.experimentRuns.getRedirectPathWithCurrentWorkspace({ projectId })
      );
    };
  }
}

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(ExperimentsList)
);
