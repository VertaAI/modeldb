import Project from 'models/Project';
import User from 'models/User';
import * as React from 'react';
import Avatar from 'react-avatar';
import { Link } from 'react-router-dom';
import SharePopup from '../SharePopup/SharePopup';
import combined from './images/combined.svg';
import styles from './ProjectWidget.module.css';

interface ILocalProps {
  project: Project;
}

interface ILocalState {
  showModal: boolean;
}

export default class ProjectWidget extends React.Component<ILocalProps, ILocalState> {
  public constructor(props: ILocalProps) {
    super(props);

    this.state = {
      showModal: false
    };

    this.showCollaborators = this.showCollaborators.bind(this);
    this.handleCloseModal = this.handleCloseModal.bind(this);
  }

  public render() {
    const project = this.props.project;

    return (
      <div>
        <SharePopup
          projectName={project.Name}
          showModal={this.state.showModal}
          onRequestClose={this.handleCloseModal}
          collaborators={[project.Author!]}
        />
        <Link className={styles.project_link} to={`/project/${project.Id}/exp-runs`}>
          <div className={styles.project_widget}>
            <div className={styles.title_block}>
              <div className={styles.title}>{project.Name}</div>
              <div className={styles.description}>{project.Description}</div>
            </div>
            <div className={styles.tags_block}>
              {project.Tags.map((tag: string, i: number) => {
                return (
                  <div className={styles.tag} key={i}>
                    <span className={styles.tag_text}>{tag}</span>
                  </div>
                );
              })}
            </div>
            <div className={styles.metrics_block} />
            <div className={styles.author_block}>
              <div className={styles.author_name}>
                <div>{project.Author ? project.Author.name : ''}</div>
                <div className={styles.author_status}>Owner</div>
              </div>
              {/* // we may use mapProjectAuthors() function from ProjectDataService.ts 
            to map project Ids to owner once backend supports author field */}
              <Avatar
                name={project.Author ? project.Author.name : ''}
                round={true}
                size="36"
                textSizeRatio={36 / 16}
                className={styles.author_avatar}
              />
            </div>
            <div className={styles.model_count_block}>
              <span className={styles.model_counter}>{Math.round(Math.random() * 10)}</span>
              <span>model</span>
            </div>
            <div className={styles.collaborators}>
              <button className={styles.collaborate_button} onClick={this.showCollaborators}>
                <img src={combined} className={styles.combined_icon} />
                <span>Collaborators</span>
              </button>
            </div>
            <div className={styles.created_date_block}>
              <div className={styles.created_date}>Created: {project.DateCreated.toLocaleDateString()}</div>
              <div>Updated: {project.DateUpdated.toLocaleDateString()}</div>
            </div>
          </div>
        </Link>
      </div>
    );
  }

  private showCollaborators(event: React.SyntheticEvent<HTMLButtonElement>) {
    event.preventDefault();
    this.setState({ showModal: true });
  }

  private handleCloseModal() {
    this.setState({ showModal: false });
  }
}
