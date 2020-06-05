import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';

import ProjectWidget from 'pages/authorized/ProjectsPages/ProjectsPage/ProjectWidget/ProjectWidget';
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
import routes, { GetRouteParams } from 'routes';
import {
  loadProjects,
  selectCommunications,
  selectProjects,
  selectProjectsPagination,
  changeProjectsPaginationWithLoading,
} from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import ProjectsPagesLayout from '../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import DeletingProjectsManager from './DeletingProjectsManager/DeletingProjectsManager';
import styles from './ProjectsPage.module.css';
import Reloading from 'core/shared/view/elements/Reloading/Reloading';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

const mapStateToProps = (state: IApplicationState) => ({
  projects: selectProjects(state),
  loadingProjects: selectCommunications(state).loadingProjects,
  filters: selectCurrentContextFilters(state),
  pagination: selectProjectsPagination(state),
  workspaceName: selectCurrentWorkspaceName(state),
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

  public componentDidMount() {
    this.loadProjects();
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
      <ProjectsPagesLayout>
        <Reloading onReload={this.loadProjects}>
          <div className={styles.root}>
            <div className={styles.actions}>
              <div className={styles.action}>
                <Button
                  to={routes.projectCreation.getRedirectPath({
                    workspaceName: match.params.workspaceName,
                  })}
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
        </Reloading>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private loadProjects() {
    this.props.dispatch(loadProjects([], this.props.workspaceName));
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
