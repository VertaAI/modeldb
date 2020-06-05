import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import CodeVersion from 'core/shared/view/domain/CodeVersion/CodeVersion';
import ProjectEntityTagsManager from 'features/tagsManager/view/ProjectEntityTagsManager/ProjectEntityTagsManager';
import { Markdown } from 'core/shared/utils/types';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes, { GetRouteParams } from 'routes';
import {
  selectLoadingProject,
  selectProject,
  selectDeletingProject,
  updateProjectReadme,
  deleteProject,
  loadProject,
} from 'store/projects';
import { IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import ProjectPageTabs from '../shared/ProjectPageTabs/ProjectPageTabs';
import MarkdownManager from './MarkdownManager/MarkdownManager';
import styles from './ProjectSummaryPage.module.css';
import ProjectEntityDescriptionManager from 'features/descriptionManager/view/ProjectEntityDescriptionManager/ProjectEntityDescriptionManager';
import SummaryInfo from 'core/shared/view/elements/SummaryViewComponents/SummaryInfo/SummaryInfo';
import Reloading from 'core/shared/view/elements/Reloading/Reloading';

const mapStateToProps = (state: IApplicationState, localProps: RouteProps) => {
  const projectId = localProps.match.params.projectId;
  return {
    project: selectProject(state, projectId),
    loadingProject: selectLoadingProject(state, projectId),
    deletingCommunication: selectDeletingProject(state, projectId),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({ updateProjectReadme, loadProject, deleteProject }, dispatch);

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.projectSummary>
>;
type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteProps;

class ProjectSummaryPage extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.loadProject();
  }
  public render() {
    const {
      project,
      match: {
        params: { projectId },
      },
      loadingProject,
      deletingCommunication,
    } = this.props;
    return (
      <ProjectsPagesLayout>
                    <Reloading onReload={this.loadProject}>
          <ProjectPageTabs
            projectId={projectId}
            isDisabled={deletingCommunication.isRequesting}
            rightContent={
              project && loadingProject.isSuccess ? (
                <DeleteFAI
                  confirmText={
                    <span>
                      You're about to delete all data associated with this
                      project.
                      <br />
                      Are you sure you want to continue?
                    </span>
                  }
                  isDisabled={deletingCommunication.isRequesting}
                  faiDataTest="delete-project-button"
                  onDelete={this.deleteProject}
                />
              ) : (
                undefined
              )
            }
          />

          <div
            className={cn(styles.root, {
              [styles.deleting]: deletingCommunication.isRequesting,
            })}
          >
            {(() => {
              if (loadingProject.isRequesting) {
                return (
                  <div className={styles.preloader}>
                    <Preloader variant="dots" />
                  </div>
                );
              }
              if (loadingProject.error || !project) {
                return <PageCommunicationError error={loadingProject.error} />;
              }
              return (
                <>
                  <div className={styles.mainInfo}>
                    <SummaryInfo
                      generalInfo={{
                        id: project.id,
                        descriptionManagerElement: (
                          <ProjectEntityDescriptionManager
                            entityId={project.id}
                            description={project.description}
                            entityType={'project'}
                          />
                        ),
                      }}
                      detailsInfo={{
                        dateCreated: project.dateCreated,
                        dateUpdated: project.dateUpdated,
                        tagsManagerElement: (
                          <ProjectEntityTagsManager
                            entityType={'project'}
                            id={project.id}
                            projectId={project.id}
                            tags={project.tags}
                            isDraggableTags={false}
                          />
                        ),
                        additionalBlock: project.codeVersion
                          ? {
                              label: 'Code Version',
                              valueElement: (
                                <CodeVersion
                                  entityType="project"
                                  entityId={project.id}
                                  codeVersion={project.codeVersion}
                                />
                              ),
                            }
                          : undefined,
                      }}
                    />
                  </div>
                  <div className={styles.readme}>
                    <MarkdownManager
                      title="README.md"
                      isEditDisabled={false}
                      initialValue={project.readme}
                      onSaveChanges={this.updateProjectReadme}
                    />
                  </div>
                </>
              );
            })()}
          </div>
        </Reloading>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private loadProject() {
    const projectId = this.props.match.params.projectId;
    this.props.loadProject(projectId);
  }

  @bind
  private deleteProject() {
    this.props.deleteProject(
      this.props.project!.id,
      this.props.match.params.workspaceName
    );
  }

  @bind
  private updateProjectReadme(readme: Markdown) {
    this.props.updateProjectReadme(this.props.match.params.projectId, readme);
  }
}

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(ProjectSummaryPage)
);
