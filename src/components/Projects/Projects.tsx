import * as React from 'react';
import { connect } from 'react-redux';

import { FilterContextPool } from 'models/FilterContextPool';
import { PropertyType } from 'models/Filters';
import { Project } from 'models/Project';
import routes from 'routes';
import { fetchProjects } from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import styles from './Projects.module.css';
import ProjectWidget from './ProjectWidget/ProjectWidget';

interface IPropsFromState {
  data?: Project[] | null;
  loading: boolean;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

FilterContextPool.registerContext({
  getMetadata: () => [
    { propertyName: 'Name', type: PropertyType.STRING },
    { propertyName: 'Description', type: PropertyType.STRING },
    { propertyName: 'Tag', type: PropertyType.STRING }
  ],

  isFilteringSupport: true,
  isValidLocation: (location: string) => {
    return location === routes.mainPage.getRedirectPath({});
  },
  name: 'Project',
  onApplyFilters: (filters, dispatch) => {
    dispatch(fetchProjects(filters));
  },
  onSearch: (text: string, dispatch) => {
    dispatch(
      fetchProjects([
        {
          invert: false,
          name: 'Name',
          type: PropertyType.STRING,
          value: text
        }
      ])
    );
  }
});

class Projects extends React.Component<AllProps> {
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
