import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';

import ProjectWidget from 'components/ProjectWidget/ProjectWidget';
import {
  IFilterContext,
  selectCurrentContextFilters,
} from 'core/features/filter';
import Button from 'core/shared/view/elements/Button/Button';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { defaultQuickFilters } from 'features/filter/Model';
import routes, { GetRouteParams } from 'routes';
import { cleanChartData } from 'store/experimentRuns';
import {
  loadProjects,
  selectCommunications,
  selectProjects,
  resetProjectsPagination,
  selectProjectsPagination,
  changeProjectsPaginationWithLoading,
  getDefaultProjectsOptions,
} from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import ProjectsPagesLayout from '../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import DeletingProjectsManager from './DeletingProjectsManager/DeletingProjectsManager';
import styles from './ProjectsPage.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  projects: selectProjects(state),
  loadingProjects: selectCommunications(state).loadingProjects,
  filters: selectCurrentContextFilters(state),
  pagination: selectProjectsPagination(state),
});

export type CurrentRoute = typeof routes.projects;
type AllProps = ReturnType<typeof mapStateToProps> &
  IConnectedReduxProps &
  RouteComponentProps<GetRouteParams<CurrentRoute>>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class ProjectsPage extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  private filterContext: IFilterContext;

  constructor(props: AllProps) {
    super(props);
    this.filterContext = {
      quickFilters: [
        defaultQuickFilters.name,
        defaultQuickFilters.description,
        defaultQuickFilters.tag,
      ],
      name: 'Projects',
      onApplyFilters: (filters, dispatch) => {
        if (this.state.isNeedResetPagination) {
          dispatch(resetProjectsPagination());
        }
        dispatch(loadProjects(filters, props.match.params.workspaceName));
        if (!this.state.isNeedResetPagination) {
          this.setState({ isNeedResetPagination: true });
        }
      },
    };
    this.props.dispatch(getDefaultProjectsOptions());
    this.props.dispatch(cleanChartData());
  }

  public render() {
    const {
      loadingProjects,
      projects,
      filters,
      pagination,
      match,
    } = this.props;

    return (
      <ProjectsPagesLayout
        filterBarSettings={{
          placeholderText: 'Drag and drop tags here',
          context: this.filterContext,
        }}
      >
        <div className={styles.root}>
          <div className={styles.actions}>
            <div className={styles.action}>
              <Button
                to={routes.projectCreation.getRedirectPathWithCurrentWorkspace(
                  {}
                )}
              >
                Create
              </Button>
            </div>
          </div>
          {(() => {
            if (loadingProjects.isRequesting) {
              return (
                <div className={styles.preloader} data-test="preloader">
                  <Preloader variant="dots" />
                </div>
              );
            }
            if (loadingProjects.error) {
              return (
                <PageCommunicationError
                  error={loadingProjects.error}
                  dataTest="error"
                />
              );
            }
            if (projects && projects.length > 0) {
              return (
                <>
                  <DeletingProjectsManager
                    workspaceName={match.params.workspaceName}
                  />
                  <div className={styles.projects} data-test="projects">
                    {projects.map((project, i) => (
                      <ProjectWidget project={project} key={i} />
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
            if (filters.length > 0 || pagination.currentPage !== 0) {
              return <NoResultsStub />;
            }
            return <NoEntitiesStub entitiesText="projects" />;
          })()}
        </div>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.dispatch(
      changeProjectsPaginationWithLoading(
        currentPage,
        this.props.filters,
        this.props.match.params.workspaceName
      )
    );
  }
}

export type IProjectsPageAllProps = AllProps;
export { ProjectsPage as ProjectsPageView };
export default connect(mapStateToProps)(ProjectsPage);
