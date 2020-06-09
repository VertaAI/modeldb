import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import { selectCurrentContextFilters } from 'features/filter';
import Button from 'shared/view/elements/Button/Button';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoResultsStub from 'shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'shared/view/elements/Pagination/Pagination';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import Reloading from 'shared/view/elements/Reloading/Reloading';
import {
  selectCommunications,
  selectProjects,
  selectProjectsPagination,
  changeProjectsPaginationWithLoading,
  getDefaultProjectsOptions,
  loadProjects,
} from 'features/projects/store';
import routes from 'shared/routes';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import DeletingProjectsManager from './DeletingProjectsManager/DeletingProjectsManager';
import styles from './Projects.module.css';
import ProjectWidget from './ProjectWidget/ProjectWidget';
import { cleanChartData } from 'features/experimentRuns/store';
import NoEntitiesStub from 'shared/view/elements/NoEntitiesStub/NoEntitiesStub';

const mapStateToProps = (state: IApplicationState) => ({
  projects: selectProjects(state),
  loadingProjects: selectCommunications(state).loadingProjects,
  filters: selectCurrentContextFilters(state),
  pagination: selectProjectsPagination(state),
  workspaceName: selectCurrentWorkspaceName(state),
});

type AllProps = ReturnType<typeof mapStateToProps> & IConnectedReduxProps;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class Projects extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  constructor(props: AllProps) {
    super(props);
    this.props.dispatch(getDefaultProjectsOptions());
    this.props.dispatch(cleanChartData());
  }

  public componentDidMount() {
    this.loadProjects();
  }

  public render() {
    const {
      loadingProjects,
      projects,
      filters,
      pagination,
      workspaceName,
    } = this.props;

    return (
      <Reloading onReload={this.loadProjects}>
        <div className={styles.root}>
          <div className={styles.actions}>
            <div className={styles.action}>
              <Button
                to={routes.projectCreation.getRedirectPath({
                  workspaceName,
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
                  <DeletingProjectsManager workspaceName={workspaceName} />
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
        this.props.workspaceName
      )
    );
  }
}

export type IProjectsAllProps = AllProps;
export { Projects as ProjectsView };
export default connect(mapStateToProps)(Projects);
