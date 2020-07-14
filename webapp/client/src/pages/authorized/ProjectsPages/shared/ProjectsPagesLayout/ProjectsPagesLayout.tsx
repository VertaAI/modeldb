import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'shared/routes';
import { selectExperimentRuns } from 'features/experimentRuns/store';
import { selectProjects } from 'features/projects/store';
import { IApplicationState } from 'setup/store/store';

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
        type: 'single',
        route: routes.projects,
        getName: () => 'Projects',
      })
      .then({
        type: 'multiple',
        routes: [
          routes.projectSummary,
          routes.experimentRuns,
          routes.charts,
          routes.experiments,
          routes.projectSettings,
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
        redirectTo: routes.projectSummary,
      })
      .thenOr([
        {
          type: 'single',
          route: routes.experimentCreation,
          getName: () => 'Experiment creation',
        },
        {
          type: 'single',
          route: routes.compareModels,
          getName: () => 'Compare models',
        },
        {
          type: 'single',
          route: routes.modelRecord,
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
