import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import ProjectEntityDescriptionManager from 'features/descriptionManager/view/ProjectEntityDescriptionManager/ProjectEntityDescriptionManager';
import ProjectEntityTagsManager from 'features/tagsManager/view/ProjectEntityTagsManager/ProjectEntityTagsManager';
import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import { Project } from 'shared/models/Project';
import routes from 'shared/routes';
import { IConnectedReduxProps } from 'setup/store/store';
import WithCopyTextIcon from 'shared/view/elements/WithCopyTextIcon/WithCopyTextIcon';

import ProjectBulkDeletion from './ProjectBulkDeletion/ProjectBulkDeletion';
import styles from './ProjectWidget.module.css';

interface ILocalProps {
  project: Project;
}

type AllProps = ILocalProps & IConnectedReduxProps;

class ProjectWidget extends React.Component<AllProps> {
  public render() {
    const { project } = this.props;

    return (
      <WithCurrentUserActionsAccesses
        entityType="project"
        entityId={project.id}
        actions={['delete', 'update']}
      >
        {({ actionsAccesses }) => (
          <ProjectBulkDeletion
            id={project.id}
            isEnabled={actionsAccesses.delete}
          >
            {togglerForDeletion => (
              <div className={cn(styles.root)} data-test="project">
                <Link
                  className={cn(styles.project_link)}
                  to={routes.projectSummary.getRedirectPathWithCurrentWorkspace(
                    {
                      projectId: project.id,
                    }
                  )}
                >
                  <div className={styles.content}>
                    <div className={styles.title_block}>
                      <div className={styles.title}>
                        <WithCopyTextIcon
                          onClick={this.preventRedirect}
                          text={project.name}
                        >
                          <span
                            className={styles.title_label}
                            data-test="project-name"
                          >
                            {project.name}
                          </span>
                        </WithCopyTextIcon>
                      </div>
                      <div>
                        <span onClick={this.preventRedirect}>
                          <ProjectEntityDescriptionManager
                            entityId={this.props.project.id}
                            entityType={'project'}
                            description={this.props.project.description}
                          />
                        </span>
                      </div>
                    </div>
                    <div className={styles.tags_block}>
                      <ProjectEntityTagsManager
                        id={project.id}
                        projectId={project.id}
                        tags={project.tags}
                        entityType="project"
                        isDraggableTags={true}
                        onClick={this.onTagsManagerClick}
                      />
                    </div>
                    <div className={styles.dates_block}>
                      <div className={styles.created_date}>
                        Created: {project.dateCreated.toLocaleDateString()}
                      </div>
                      <div>
                        Updated: {project.dateUpdated.toLocaleDateString()}
                      </div>
                    </div>
                    <div className={styles.actions}>
                      {togglerForDeletion && (
                        <div
                          className={cn(styles.action, {
                            [styles.action_delete]: true,
                          })}
                          onClick={this.preventRedirect}
                        >
                          {togglerForDeletion}
                        </div>
                      )}
                    </div>
                  </div>
                </Link>
              </div>
            )}
          </ProjectBulkDeletion>
        )}
      </WithCurrentUserActionsAccesses>
    );
  }

  @bind
  private onTagsManagerClick(e: React.MouseEvent, byEmptiness: boolean) {
    if (!byEmptiness) {
      this.preventRedirect(e);
    }
  }

  @bind
  private preventRedirect(e: React.MouseEvent) {
    e.preventDefault();
  }
}

export default connect()(ProjectWidget);
