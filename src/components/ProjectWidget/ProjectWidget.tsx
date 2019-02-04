import Project from 'models/Project';
import * as React from 'react';
import Avatar from 'react-avatar';
import { Link } from 'react-router-dom';
import styles from './ProjectWidget.module.css';

interface ILocalProps {
  project: Project;
}

export default class ProjectWidget extends React.Component<ILocalProps> {
  public render() {
    return (
      <Link className={styles.project_link} to={`/project/${this.props.project.Id}/exp-runs`}>
        <div className={styles.project_widget}>
          <div className={styles.title_block}>
            <div className={styles.title}>{this.props.project.Name}</div>
            <div className={styles.description}>{this.props.project.Description}</div>
            <div className={styles.tags_block}>
              {this.props.project.Tags.map((tag: string, i: number) => {
                return (
                  <p key={i} className={styles.tags}>
                    {tag}
                  </p>
                );
              })}
            </div>
            <div>
              <div className={styles.model_counter}>{Math.round(Math.random() * 10)}</div>
              <div className={styles.inline_block}>model</div>
            </div>
          </div>
          <div className={styles.metrics_block} />
          <div className={styles.author_block}>
            <div>
              <div>Manasi Vartak</div>
              <div className={styles.semitransparent}>Owner</div>
            </div>

            {/* // we may use mapProjectAuthors() function from ProjectDataService.ts to map project Ids to owner once backend supports author field */}
            <Avatar
              name="Manasi Vartak"
              round={true}
              size="36"
              textSizeRatio={36 / 16}
              style={{ fontFamily: 'Roboto', fontWeight: '300' }}
            />
            <div className={styles.created_date_block}>
              <div>Created:</div>
              <div>{this.props.project.DateCreated.toLocaleDateString()}</div>
              <br />
              <div>Updated:</div>
              <div>{this.props.project.DateUpdated.toLocaleDateString()}</div>
            </div>
          </div>
        </div>
      </Link>
    );
  }
}
