import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';
import { IPages } from 'pages/authorized/types';

import ChartsPage from './ChartsPage/ChartsPage';
import CompareModelsPage from './CompareModelsPage/CompareModelsPage';
import ExperimentRunsPage from './ExperimentRunsPage/ExperimentRunsPage';
import ExperimentsPage from './ExperimentsPage/ExperimentsPage';
import ModelRecordPage from './ModelRecordPage/ModelRecordPage';
import ProjectSummaryPage from './ProjectSummaryPage/ProjectSummaryPage';
import { withLoadProject } from './LoadProject';
import ExperimentCreationPage from './ExperimentCreationPage/ExperimentCreationPage';

export const projectDetailsPages: IPages = {
  getPages: () => [
    <Route
      exact={true}
      key={routes.projectSummary.getPath()}
      path={routes.projectSummary.getPath()}
      component={withLoadProject(ProjectSummaryPage)}
    />,
    <Route
      exact={true}
      key={routes.charts.getPath()}
      path={routes.charts.getPath()}
      component={withLoadProject(ChartsPage)}
    />,
    <Route
      exact={true}
      key={routes.experimentRuns.getPath()}
      path={routes.experimentRuns.getPath()}
      component={withLoadProject(ExperimentRunsPage)}
    />,
    <Route
      exact={true}
      key={routes.experiments.getPath()}
      path={routes.experiments.getPath()}
      component={withLoadProject(ExperimentsPage)}
    />,
    <Route
      exact={true}
      key={routes.experimentCreation.getPath()}
      path={routes.experimentCreation.getPath()}
      component={ExperimentCreationPage}
    />,
    <Route
      exact={true}
      key={routes.modelRecord.getPath()}
      path={routes.modelRecord.getPath()}
      component={withLoadProject(ModelRecordPage)}
    />,
    <Route
      exact={true}
      key={routes.compareModels.getPath()}
      path={routes.compareModels.getPath()}
      component={withLoadProject(CompareModelsPage)}
    />,
  ],
};
