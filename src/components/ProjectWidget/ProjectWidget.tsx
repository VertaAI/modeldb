import Project from 'models/Project';
import * as React from 'react';
import Avatar from 'react-avatar';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './ProjectWidget.module.css';

interface IOwnProps {
  project: Project;
}

type AllProps = IOwnProps & IConnectedReduxProps;

class ProjectWidget extends React.Component<AllProps> {
  public render() {
    return (
      <Link className={styles.project_link} to={`/project/${this.props.project.Id}/models`}>
        <div className={styles.project_widget}>
          <div className={styles.title_block}>
            <div className={styles.title}>{this.props.project.Name}</div>
            <div className={styles.description}>{this.props.project.Description}</div>
            <div>
              <div className={styles.model_counter}>{this.props.project.Models.length}</div>
              <div className={styles.inline_block}>model</div>
            </div>
          </div>
          <div className={styles.author_block}>
            <div>
              <div>{this.props.project.Author}</div>
              <div className={styles.semitransparent}>Owner</div>
            </div>

            <Avatar
              name={this.props.project.Author}
              round={true}
              size="36"
              textSizeRatio={36 / 16}
              style={{ fontFamily: 'Roboto', fontWeight: '300' }}
            />
            <div className={styles.created_date_block}>
              <div>Created:</div>
              <div>{this.props.project.CreationDate.toLocaleDateString()}</div>
            </div>
          </div>
        </div>
      </Link>
    );
  }
}

const mapStateToProps = ({ projects }: IApplicationState) => ({
  loading: projects.loading
});

export default connect<{}, {}, {}, IApplicationState>(mapStateToProps)(ProjectWidget);
