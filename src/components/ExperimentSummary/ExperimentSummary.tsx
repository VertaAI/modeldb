import React from 'react';
import { Route, RouteComponentProps, RouteProps, Switch, withRouter } from 'react-router-dom';
import Charts from '../Charts/Charts';
import routes from '../../routes';
import ExperimentRuns from '../ExperimentRuns/ExperimentRuns';
import { GenericNotFound } from '../GenericNotFound/GenericNotFound';
import ExpSubMenu from '../SubMenu/ExpSubMenu';

class ExperimentSummary extends React.Component<RouteComponentProps> {
  public render() {
    return (
      <div>
        <div>
          <ExpSubMenu />
        </div>
        <Switch>
          <Route path={routes.expirementRuns.getPath()} component={ExperimentRuns} />
          <Route path={routes.charts.getPath()} component={Charts} />
          <Route component={GenericNotFound} />
        </Switch>
      </div>
    );
  }
}

export default withRouter(ExperimentSummary);
