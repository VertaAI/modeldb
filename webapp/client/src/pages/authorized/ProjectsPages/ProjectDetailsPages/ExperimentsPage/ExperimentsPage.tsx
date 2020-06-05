import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';
import routes, { GetRouteParams } from 'routes';

import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import {
  selectCurrentContextAppliedFilters,
  updateContextFilters,
} from 'core/features/filter';
import {
  makeDefaultExprNameFilter,
  PropertyType,
} from 'core/features/filter/Model';
import Button from 'core/shared/view/elements/Button/Button';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import Experiment from 'models/Experiment';
import {
  changeExperimentsPaginationWithLoading,
  getDefaultExperimentsOptions,
  loadExperiments,
  resetExperimentsPagination,
  selectExperiments,
  selectExperimentsPagination,
  selectLoadingExperiments,
} from 'store/experiments';
import { selectProject } from 'features/projects/store';
import { IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import makeExprRunsFilterContextName from '../shared/makeExprRunsFilterContextName';
import ProjectPageTabs from '../shared/ProjectPageTabs/ProjectPageTabs';
import DeletingExperimentsManager from './DeletingExperimentsManager/DeletingExperimentsManager';
import styles from './ExperimentsPage.module.css';
import ExperimentWidget from './ExperimentWidget/ExperimentWidget';
import Reloading from 'core/shared/view/elements/Reloading/Reloading';

const mapStateToProps = (state: IApplicationState, localProps: RouteProps) => {
  return {
    experiments: selectExperiments(state),
    project: selectProject(state, localProps.match.params.projectId),
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

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.experiments>
>;
type AllProps = RouteProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class ExperimentsPage extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  constructor(props: AllProps) {
    super(props);
    const projectId = this.props.match.params.projectId;
    this.props.getDefaultExperimentsOptions(projectId);
  }

  public componentDidMount() {
    this.loadProject();
  }

  public render() {
    const {
      experiments,
      loadingExperimentsCommunication,
      pagination,
      match: {
        params: { projectId },
      },
      filters,
    } = this.props;
    return (
      <ProjectsPagesLayout>
        <Reloading onReload={this.loadProject}>
          <div className={styles.root}>
            <WithCurrentUserActionsAccesses
              entityType="project"
              entityId={projectId}
              actions={['update']}
            >
              {({ actionsAccesses }) => (
                <ProjectPageTabs
                  projectId={projectId}
                  rightContent={
                    actionsAccesses.update ? (
                      <Button
                        to={routes.experimentCreation.getRedirectPathWithCurrentWorkspace(
                          { projectId }
                        )}
                      >
                        Create Experiment
                      </Button>
                    ) : null
                  }
                />
              )}
            </WithCurrentUserActionsAccesses>
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
                          onCurrentPageChange={
                            this.onPaginationCurrentPageChange
                          }
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
          </div>
        </Reloading>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private loadProject() {
    const projectId = this.props.match.params.projectId;
    this.props.loadExperiments(projectId, []);
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.changeExperimentsPaginationWithLoading(
      this.props.match.params.projectId,
      currentPage,
      this.props.filters
    );
  }

  @bind
  private makeOnViewExprRuns(experiment: Experiment) {
    return () => {
      const {
        match: {
          params: { projectId },
        },
      } = this.props;
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

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(ExperimentsPage));
