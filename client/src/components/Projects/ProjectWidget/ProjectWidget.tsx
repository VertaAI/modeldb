import * as React from 'react';
import Avatar from 'react-avatar';
import { Link } from 'react-router-dom';

import { PropertyType } from 'models/Filters';
import { Project } from 'models/Project';

import Draggable from 'components/Draggable/Draggable';
import routes from 'routes';
import styles from './ProjectWidget.module.css';

interface ILocalProps {
  project: Project;
}

export default class ProjectWidget extends React.Component<ILocalProps> {
  public render() {
    const project = this.props.project;

    return (
      <Link className={styles.project_link} to={routes.expirementRuns.getRedirectPath({ projectId: project.id })}>
        <div className={styles.project_widget}>
          <div className={styles.title_block}>
            <div className={styles.title}>{project.name}</div>
            <div className={styles.description}>{project.description}</div>
          </div>
          <div className={styles.tags_block}>
            {this.props.project.tags &&
              this.props.project.tags.map((tag: string, i: number) => {
                return (
                  <Draggable
                    key={i}
                    type="filter"
                    data={{ type: PropertyType.STRING, name: 'Tag', value: tag }}
                    additionalClassName={styles.tag}
                  >
                    <span className={styles.tag_text}>{tag}</span>
                  </Draggable>
                );
              })}
          </div>
          <div className={styles.metrics_block} />
          <div className={styles.author_block}>
            <div className={styles.author_name}>
              <div>{project.Author.getNameOrEmail()}</div>
              <div className={styles.author_status}>Owner</div>
            </div>
            <Avatar
              name={project.Author.getNameOrEmail()}
              round={true}
              size="36"
              textSizeRatio={36 / 16}
              className={styles.author_avatar}
              src={project.Author.picture ? project.Author.picture : ''}
            />
          </div>
          <div className={styles.model_count_block}>
            <span className={styles.model_counter}>{12}</span>
            <span>model</span>
          </div>
          <div className={styles.created_date_block}>
            <div className={styles.created_date}>Created: {project.dateCreated.toLocaleDateString()}</div>
            <div>Updated: {project.dateUpdated.toLocaleDateString()}</div>
          </div>
        </div>
      </Link>
    );
  }
}
