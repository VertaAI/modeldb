import React from 'react';
import {
  Route,
  RouteComponentProps,
  Switch,
  withRouter,
} from 'react-router-dom';

import { FilterContextPool } from 'models/FilterContextPool';
import { PropertyType } from 'models/Filters';
import routes, { GetRouteParams, IRoute } from 'routes';
import { fetchExperimentRuns } from 'store/experiment-runs';

import Charts from '../Charts/Charts';
import ExperimentRuns from '../ExperimentRuns/ExperimentRuns';
import ProjectPageTabs from '../ProjectPageTabs/ProjectPageTabs';

let currentProjectID: string;
FilterContextPool.registerContext({
  getMetadata: () => [
    { propertyName: 'Name', type: PropertyType.STRING },
    { propertyName: 'Tag', type: PropertyType.STRING },
  ],
  isFilteringSupport: true,
  isValidLocation: (location: string) => {
    for (const route of [routes.experimentRuns, routes.charts]) {
      const match = route.getMatch(location);
      if (match) {
        currentProjectID = match.projectId;
        return true;
      }
    }
    return false;
  },
  name: 'ModelRecord',
  onApplyFilters: (filters, dispatch) => {
    dispatch(fetchExperimentRuns(currentProjectID, filters));
  },
  onSearch: (text: string, dispatch) => {
    dispatch(
      fetchExperimentRuns(currentProjectID, [
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

type IUrlProps = GetRouteParams<typeof routes.experimentRuns>;
class ExperimentSummary extends React.Component<
  RouteComponentProps<IUrlProps>
> {
  public render() {
    const projectId = this.props.match.params.projectId;

    let currentRoute: IRoute<any>;

    for (const [key, route] of Object.entries(routes)) {
      if (route.getPath() === this.props.match.path) {
        currentRoute = route;
      }
    }

    return (
      <React.Fragment>
        <ProjectPageTabs projectId={projectId} activeRoute={currentRoute!} />
        <Switch>
          <Route
            path={routes.experimentRuns.getPath()}
            component={ExperimentRuns}
          />
          <Route path={routes.charts.getPath()} component={Charts} />
        </Switch>
      </React.Fragment>
    );
  }
}

export default withRouter(ExperimentSummary);
