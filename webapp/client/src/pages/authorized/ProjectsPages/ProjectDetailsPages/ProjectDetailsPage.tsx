import React from 'react';
import { connect } from 'react-redux';
import {
  Route,
  RouteComponentProps,
  Switch,
  withRouter,
} from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import routes, { GetRouteParams } from 'shared/routes';
import {
  loadProject,
  selectProject,
  selectLoadingProjectWithMemo,
} from 'features/projects/store';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import ChartsPage from './ChartsPage/ChartsPage';
import CompareModelsPage from './CompareModelsPage/CompareModelsPage';
import ExperimentRunsPage from './ExperimentRunsPage/ExperimentRunsPage';
import ExperimentsPage from './ExperimentsPage/ExperimentsPage';
import ModelRecordPage from './ModelRecordPage/ModelRecordPage';
import ProjectSummaryPage from './ProjectSummaryPage/ProjectSummaryPage';

type IUrlProps = GetRouteParams<typeof routes.experimentRuns>;

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      loadProject,
    },
    dispatch
  );

const mapStateToProps = (state: IApplicationState, localProps: RouteProps) => {
  return {
    loadingProject: selectLoadingProjectWithMemo(
      state,
      localProps.match.params.projectId
    ),
    project: selectProject(state, localProps.match.params.projectId),
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type RouteProps = RouteComponentProps<IUrlProps>;

type AllProps = RouteProps &
  ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

class ProjectDetailsPage extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.loadProject(this.props.match.params.projectId);
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (prevProps.project && !this.props.project) {
      this.props.history.replace(
        routes.projects.getRedirectPath({
          workspaceName: this.props.workspaceName,
        })
      );
    }
  }

  public render() {
    const { loadingProject, project } = this.props;
    return matchRemoteData(loadingProject, project, {
      notAsked: () => (
        <AuthorizedLayout>
          <Preloader variant="dots" />
        </AuthorizedLayout>
      ),
      requesting: () => (
        <AuthorizedLayout>
          <Preloader variant="dots" />
        </AuthorizedLayout>
      ),
      errorOrNillData: ({ error }) => (
        <AuthorizedLayout>
          <PageCommunicationError error={error} />
        </AuthorizedLayout>
      ),
      success: () => (
        <Switch>
          <Route
            exact={true}
            path={routes.projectSummary.getPath()}
            component={ProjectSummaryPage}
          />
          <Route
            exact={true}
            path={routes.charts.getPath()}
            component={ChartsPage}
          />
          <Route
            exact={true}
            path={routes.experimentRuns.getPath()}
            component={ExperimentRunsPage}
          />
          <Route
            exact={true}
            path={routes.experiments.getPath()}
            component={ExperimentsPage}
          />
          <Route
            exact={true}
            path={routes.modelRecord.getPath()}
            component={ModelRecordPage}
          />
          <Route
            exact={true}
            path={routes.compareModels.getPath()}
            component={CompareModelsPage}
          />
        </Switch>
      ),
    });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(ProjectDetailsPage));
