import * as React from 'react';
import { connect } from 'react-redux';

import Preloader from 'components/shared/Preloader/Preloader';
import { FilterContextPool } from 'models/FilterContextPool';
import { PropertyType } from 'models/Filters';
import { Project } from 'models/Project';
import routes from 'routes';
import {
  fetchProjects,
  selectIsLoadingProjects,
  selectProjects,
} from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import DeveloperKeyManager from './DeveloperKeyManager/DeveloperKeyManager';

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
    { propertyName: 'Tag', type: PropertyType.STRING },
  ],

  isFilteringSupport: true,
  isValidLocation: (location: string) => {
    return routes.mainPage.getMatch(location) !== null;
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
          value: text,
        },
      ])
    );
  },
});

class Projects extends React.PureComponent<AllProps> {
  public render() {
    const { loading, data } = this.props;
    return (
      <div className={styles.projects}>
        <div className={styles.widgets_list}>
          {(() => {
            if (loading) {
              return <Preloader variant="dots" />;
            }
            if (data && data.length !== 0) {
              return data.map((proj, i) => (
                <ProjectWidget project={proj} key={i} />
              ));
            }
            return <DeveloperKeyManager />;
          })()}
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  data: selectProjects(state),
  loading: selectIsLoadingProjects(state),
});

export default connect(mapStateToProps)(Projects);
