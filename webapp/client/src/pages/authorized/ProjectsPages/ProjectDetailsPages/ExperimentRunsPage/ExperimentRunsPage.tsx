import * as React from 'react';

import ExperimentRuns from 'components/ExperimentRuns/ExperimentRuns';
import { RouteComponentProps, withRouter } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import LayoutWithExprRunsFilter from '../shared/LayoutWithExprRunsFilter/LayoutWithExprRunsFilter';
import styles from './ExperimentRunsPage.module.css';

type AllProps = RouteComponentProps<
  GetRouteParams<typeof routes.experimentRuns>
>;

class ExperimentRunsPage extends React.PureComponent<AllProps> {
  public render() {
    const projectId = this.props.match.params.projectId;
    return (
      <LayoutWithExprRunsFilter>
        <div className={styles.root}>
          <div className={styles.experimentRuns}>
            <ExperimentRuns projectId={projectId} />
          </div>
        </div>
      </LayoutWithExprRunsFilter>
    );
  }
}

export default withRouter(ExperimentRunsPage);
