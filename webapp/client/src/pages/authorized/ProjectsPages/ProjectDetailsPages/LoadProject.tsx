import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import routes, { GetRouteParams } from 'routes';
import { matchRemoteData } from 'core/shared/utils/redux/communication/remoteData';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import {
  loadProject,
  selectLoadingProject,
  selectProject,
} from 'store/projects';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';
import makeWrapperComponent from 'core/shared/view/elements/makeWrapperComponent';

type IUrlProps = GetRouteParams<typeof routes.project>;

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      loadProject,
    },
    dispatch
  );

const mapStateToProps = (state: IApplicationState, localProps: RouteProps) => {
  return {
    loadingProject: selectLoadingProject(
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
  ReturnType<typeof mapStateToProps> & {
    children: Exclude<React.ReactNode, null | undefined>;
  };

class LoadProject extends React.Component<AllProps> {
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
      success: () => this.props.children,
    });
  }
}

const ConnectedLoadProject = withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(LoadProject)
);

export const withLoadProject = makeWrapperComponent(ConnectedLoadProject);

export default ConnectedLoadProject;
