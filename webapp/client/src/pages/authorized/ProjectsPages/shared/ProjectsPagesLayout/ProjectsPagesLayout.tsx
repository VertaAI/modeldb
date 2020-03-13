import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';
import { selectExperimentRuns } from 'store/experimentRuns';
import { selectProjects } from 'store/projects';
import { IApplicationState } from 'store/store';

type ILocalProps = IAuthorizedLayoutLocalProps;

const mapStateToProps = (state: IApplicationState) => ({
  experimentRuns: selectExperimentRuns(state),
  projects: selectProjects(state),
});

type AllProps = ReturnType<typeof mapStateToProps> & ILocalProps;

class ProjectsPagesLayout extends React.Component<AllProps> {
  public render() {
    const { filterBarSettings, children } = this.props;
    return (
      <AuthorizedLayout
        breadcrumbsBuilder={this.getBreadcrumbsBuilder()}
        filterBarSettings={filterBarSettings}
      >
        {children}
      </AuthorizedLayout>
    );
  }

  @bind
  private getBreadcrumbsBuilder() {
    const { projects, experimentRuns = [] } = this.props;

    return BreadcrumbsBuilder()
      .then({
        routes: [routes.projects],
        getName: () => 'Projects',
      })
      .then({
        routes: [
          routes.projectSummary,
          routes.experimentRuns,
          routes.charts,
          routes.experiments,
        ],
        checkLoaded: params =>
          Boolean(
            projects &&
              projects.some(project => project.id === params.projectId)
          ),
        getName: params => {
          const targetProject = (projects || []).find(
            project => project.id === params.projectId
          )!;
          return targetProject ? targetProject.name : '';
        },
      })
      .thenOr([
        {
          routes: [routes.experimentCreation],
          getName: () => 'Experiment creation',
        },
        {
          routes: [routes.compareModels],
          getName: () => 'Compare models',
        },
        {
          routes: [routes.modelRecord],
          checkLoaded: ({ modelRecordId }) => {
            const experimentRun = (experimentRuns || []).find(
              exprRun => exprRun.id === modelRecordId
            );
            return Boolean(experimentRun);
          },
          getName: ({ modelRecordId }) => {
            const experimentRun = (experimentRuns || []).find(
              exprRun => exprRun.id === modelRecordId
            );
            return experimentRun ? experimentRun.name : '';
          },
        },
      ]);
  }
}

export default connect(mapStateToProps)(ProjectsPagesLayout);
