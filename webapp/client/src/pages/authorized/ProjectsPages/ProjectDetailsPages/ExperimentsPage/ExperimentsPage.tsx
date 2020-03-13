import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';
import routes, { GetRouteParams } from 'routes';

import ExperimentWidget from 'components/ExperimentWidget/ExperimentWidget';
import {
  IFilterContext,
  selectCurrentContextAppliedFilters,
  updateContextFilters,
} from 'core/features/filter';
import {
  makeDefaultExprNameFilter,
  PropertyType,
} from 'core/features/filter/Model';
import Button from 'core/shared/view/elements/Button/Button';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import Experiment from 'models/Experiment';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import {
  changeExperimentsPaginationWithLoading,
  getDefaultExperimentsOptions,
  loadExperiments,
  resetExperimentsPagination,
  selectExperiments,
  selectExperimentsPagination,
  selectLoadingExperiments,
} from 'store/experiments';
import { defaultQuickFilters } from 'features/filter/Model';
import { selectProject } from 'store/projects';
import { IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import makeExprRunsFilterContextName from '../shared/makeExprRunsFilterContextName';
import ProjectPageTabs from '../shared/ProjectPageTabs/ProjectPageTabs';
import DeletingExperimentsManager from './DeletingExperimentsManager/DeletingExperimentsManager';
import styles from './ExperimentsPage.module.css';

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

  private filterContext: IFilterContext;

  constructor(props: AllProps) {
    super(props);
    const projectId = this.props.match.params.projectId;
    const contextName = `Experiments-${projectId}`;
    this.filterContext = {
      quickFilters: [
        defaultQuickFilters.name,
        defaultQuickFilters.description,
        defaultQuickFilters.tag,
      ],
      name: contextName,
      onApplyFilters: filters => {
        if (this.state.isNeedResetPagination) {
          this.props.resetExperimentsPagination(projectId);
        }
        this.props.loadExperiments(projectId, filters);
        if (!this.state.isNeedResetPagination) {
          this.setState({ isNeedResetPagination: true });
        }
      },
    };
    this.props.getDefaultExperimentsOptions(projectId);
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
      <ProjectsPagesLayout
        filterBarSettings={{
          context: this.filterContext,
          placeholderText: 'Drag and drop tags here',
        }}
      >
        <div className={styles.root}>
          <ProjectPageTabs
            projectId={projectId}
            rightContent={
              <Button
                to={routes.experimentCreation.getRedirectPathWithCurrentWorkspace(
                  { projectId }
                )}
              >
                Create Experiment
              </Button>
            }
          />
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
        </div>
      </ProjectsPagesLayout>
    );
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
