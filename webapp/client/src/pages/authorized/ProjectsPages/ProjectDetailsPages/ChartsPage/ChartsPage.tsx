import { bind } from 'decko';
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import Charts from 'components/Charts/Charts';

import LayoutWithExprRunsFilter from '../shared/LayoutWithExprRunsFilter/LayoutWithExprRunsFilter';

type AllProps = RouteComponentProps<
  GetRouteParams<typeof routes.experimentRuns>
>;

interface ILocalState {
  isShowCharts: boolean;
}

class ChartsPage extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = { isShowCharts: true };

  public render() {
    return (
      <LayoutWithExprRunsFilter>
        {this.state.isShowCharts && (
          <Charts
            projectId={this.props.match.params.projectId}
            onResetConfigurations={this.onResetConfigurations}
          />
        )}
      </LayoutWithExprRunsFilter>
    );
  }

  @bind
  private onResetConfigurations() {
    this.setState({
      isShowCharts: false,
    });
    setTimeout(() => {
      this.setState({ isShowCharts: true });
    }, 5);
  }
}

export default withRouter(ChartsPage);
