import Project from 'models/Project';
import * as React from 'react';
import { connect } from 'react-redux';
import { apiFetchProjects } from '../../store/project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import ProjectWidget from '../ProjectWidget/ProjectWidget';
import styles from './Projects.module.css';

// fetchProjects,

interface IPropsFromState {
  projects?: Project[] | null;
  apiProjects?: any | null;
  loading: boolean;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Projects extends React.Component<AllProps> {
  public componentDidMount() {
    // this.props.dispatch(fetchProjects());
    this.props.dispatch(apiFetchProjects());
  }

  public render() {
    return (
      <div className={styles.projects}>
        <div className={styles.widgets_list}>
          {console.log(this.props.apiProjects)}
          {this.props.apiProjects ? this.props.apiProjects.map((proj: any, i: number) => <ProjectWidget project={proj} key={i} />) : ''}
        </div>
      </div>
    );
  }
}
// projects,
const mapStateToProps = ({ apiProjects }: IApplicationState) => ({
  loading: apiProjects.loading,
  // projects: projects.data,
  apiProjects: apiProjects.data
});

export default connect(mapStateToProps)(Projects);
