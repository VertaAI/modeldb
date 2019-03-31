import React from 'react';
import {
  Route,
  RouteComponentProps,
  RouteProps,
  Switch,
  withRouter,
} from 'react-router-dom';
import routes from '../../routes';
import ExperimentSummary from '../ExperimentSummary/ExperimentSummary';
import { FilterSelect } from '../FilterSelect/FilterSelect';
import { GenericNotFound } from '../GenericNotFound/GenericNotFound';
import ModelRecord from '../ModelRecord/ModelRecord';
import Projects from '../Projects/Projects';
import styles from './AuthorizedLayout.module.css';
import AuthorizedLayoutHeader from './AuthorizedLayoutHeader/AuthorizedLayoutHeader';

// tslint:disable-next-line:variable-name
export const RouteWithFilter = ({ component, ...rest }: RouteProps) => {
  if (!component) {
    throw Error('component is undefined');
  }

  // tslint:disable-next-line:variable-name
  const Component = component; // JSX Elements have to be uppercase.
  const render = (props: RouteComponentProps<any>): React.ReactNode => {
    return (
      <div className={styles.content_area}>
        <div className={styles.filters_bar}>
          <FilterSelect placeHolderText="Search models and filters" />
        </div>
        <div className={styles.content}>
          <Component {...props} />
        </div>
      </div>
    );
  };

  return <Route {...rest} render={render} />;
};

class AuthorizedLayout extends React.Component<RouteComponentProps> {
  public render() {
    return (
      <div className={styles.layout}>
        <div className={styles.header}>
          <AuthorizedLayoutHeader />
        </div>
        <Switch>
          <RouteWithFilter
            exact={true}
            path={routes.mainPage.getPath()}
            component={Projects}
          />
          <RouteWithFilter
            path={routes.modelRecord.getPath()}
            component={ModelRecord}
          />
          <RouteWithFilter
            path={routes.expirementRuns.getPath()}
            component={ExperimentSummary}
          />
          <RouteWithFilter
            path={routes.charts.getPath()}
            component={ExperimentSummary}
          />
          {/* <RouteWithFilter path={'/'} component={ExperimentSummary} /> */}

          <Route component={GenericNotFound} />
        </Switch>
      </div>
    );
  }
}

export default withRouter(AuthorizedLayout);
