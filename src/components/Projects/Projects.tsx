import Project from 'models/Project';
import * as React from 'react';
import { connect } from 'react-redux';
import { fetchProjects, apiFetchProjects } from '../../store/project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import ProjectWidget from '../ProjectWidget/ProjectWidget';
import styles from './Projects.module.css';

interface IPropsFromState {
  projects?: Project[] | null;
  loading: boolean;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Projects extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(fetchProjects());
    this.props.dispatch(apiFetchProjects());
  }

  public render() {
    return (
      <div className={styles.projects}>
        <div className={styles.widgets_list}>
          {this.props.projects ? this.props.projects.map((proj, i) => <ProjectWidget project={proj} key={i} />) : ''}
        </div>
      </div>
    );
  }
}

const mapStateToProps = ({ projects, apiProjects }: IApplicationState) => ({
  loading: projects.loading,
  projects: projects.data,
  apiProjects: apiProjects.data
});

export default connect(mapStateToProps)(Projects);
