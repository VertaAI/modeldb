import Project from 'models/Project';
import * as React from 'react';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './ProjectWidget.module.css';

interface IOwnProps {
  project: Project;
}

type AllProps = IOwnProps & IConnectedReduxProps;

class ProjectWidget extends React.Component<AllProps> {
  public render() {
    return (
      <div className={styles.project_widget}>
        <div className={styles.title_block}>
          <div className={styles.title}>{this.props.project.Name}</div>
          <div>{this.props.project.Description}</div>
          <div>
            <div className={styles.model_counter}>
              {this.props.project.Models.length}
            </div>
            <div className={styles.inline_block}>model</div>
          </div>
        </div>
        <div className={styles.metrics_block}>
          <div className={styles.metrics_header}>
            <div className={styles.metrics_header_item}>Metrics</div>
            <div className={styles.metrics_header_item}>min</div>
            <div className={styles.metrics_header_item}>max</div>
            <div className={styles.metrics_header_item}>average</div>
          </div>
        </div>
        <div className={styles.author_block} />
      </div>
    );
  }
}

const mapStateToProps = ({ projects }: IApplicationState) => ({
  loading: projects.loading
});

export default connect<{}, {}, {}, IApplicationState>(mapStateToProps)(
  ProjectWidget
);
