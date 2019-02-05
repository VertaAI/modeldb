import * as React from 'react';
import { connect } from 'react-redux';
import { fetchProjects } from '../../store/projects';
import { initContext, resetContext } from '../../store/filter/actions';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import Project from '../../models/Project';
import ProjectWidget from '../ProjectWidget/ProjectWidget';
import styles from './Projects.module.css';

interface IPropsFromState {
  data?: Project[] | null;
  loading: boolean;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Projects extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(fetchProjects());
    this.props.dispatch(
      initContext(Project.name, {
        appliedFilters: [],
        ctx: Project.name,
        isFiltersSupporting: true,
        metadata: Project.metaData
      })
    );
  }

  public componentWillUnmount() {
    this.props.dispatch(resetContext());
  }

  public render() {
    return (
      <div className={styles.projects}>
        <div className={styles.widgets_list}>
          {this.props.data ? this.props.data.map((proj, i) => <ProjectWidget project={proj} key={i} />) : ''}
        </div>
      </div>
    );
  }
}

const mapStateToProps = ({ projects }: IApplicationState) => ({
  data: projects.data,
  loading: projects.loading
});

export default connect(mapStateToProps)(Projects);
