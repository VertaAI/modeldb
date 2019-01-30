import Project from 'models/Project';
import * as React from 'react';
import Avatar from 'react-avatar';
import { Link } from 'react-router-dom';
import styles from './ProjectWidget.module.css';

interface ILocalProps {
  project: any;
}

export default class ProjectWidget extends React.Component<ILocalProps> {
  public render() {
    return (
      <Link className={styles.project_link} to={`/project/${this.props.project.Id}/models`}>
        <div className={styles.project_widget}>
          <div className={styles.title_block}>
            <div className={styles.title}>{this.props.project.name}</div>
            <div className={styles.description}>{this.props.project.description}</div>
            <div>
              <div className={styles.model_counter}>{Math.round(Math.random() * 10)}</div>
              <div className={styles.inline_block}>model</div>
            </div>
          </div>
          <div className={styles.author_block}>
            <div>
              <div>Manasi Vartak</div>
              <div className={styles.semitransparent}>Owner</div>
            </div>

            <Avatar
              name="Manasi Vartak"
              round={true}
              size="36"
              textSizeRatio={36 / 16}
              style={{ fontFamily: 'Roboto', fontWeight: '300' }}
            />
            <div className={styles.created_date_block}>
              <div>Created:</div>
              <div> {getDate(Number(this.props.project.date_created))} </div>
              <br />
              <div>Updated:</div>
              <div> {getDate(Number(this.props.project.date_updated))} </div>
            </div>
          </div>
        </div>
      </Link>
    );
  }
}

const getDate = function(utc_date: number): string {
  const date = new Date(utc_date);
  return date
    .toUTCString()
    .split(' ')
    .slice(0, 4)
    .join(' ');
};
