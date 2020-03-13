import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import ProjectEntityDescriptionManager from 'components/DescriptionManager/ProjectEntityDescriptionManager/ProjectEntityDescriptionManager';
import ProjectEntityTagsManager from 'components/TagsManager/ProjectEntityTagsManager/ProjectEntityTagsManager';
import CopyToClipboard from 'core/shared/view/elements/CopyToClipboard/CopyToClipboard';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import { Project } from 'models/Project';
import routes from 'routes';
import { IConnectedReduxProps } from 'store/store';

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
      <ProjectBulkDeletion id={project.id} isEnabled={true}>
        {togglerForDeletion => (
          <div className={cn(styles.root)} data-test="project">
            <Link
              className={cn(styles.project_link)}
              to={routes.projectSummary.getRedirectPathWithCurrentWorkspace({
                projectId: project.id,
              })}
            >
              <div className={styles.content}>
                <div className={styles.title_block}>
                  <div className={styles.title}>
                    <span
                      className={styles.title_label}
                      data-test="project-name"
                    >
                      {project.name}
                    </span>
                    <span
                      className={styles.title_copy_icon}
                      onClick={this.preventRedirect}
                    >
                      <CopyToClipboard text={project.name}>
                        {(onCopy: any) => (
                          <Icon type={'copy-to-clipboard'} onClick={onCopy} />
                        )}
                      </CopyToClipboard>
                    </span>
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
                  <div>Updated: {project.dateUpdated.toLocaleDateString()}</div>
                </div>
                <div className={styles.actions}>
                  {togglerForDeletion && (
                    <div
                      className={styles.action}
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
    );
  }

  @bind
  private onTagsManagerClick(e: React.MouseEvent, byEmptiness: boolean) {
    !byEmptiness ? this.preventRedirect(e) : undefined;
  }

  @bind
  private preventRedirect(e: React.MouseEvent) {
    e.preventDefault();
  }
}

export default connect()(ProjectWidget);
