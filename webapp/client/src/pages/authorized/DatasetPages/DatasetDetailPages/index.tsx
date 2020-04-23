import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';

import CompareDatasetVersionsPage from './CompareDatasetVersionsPage/CompareDatasetVersionsPage';
import DatasetSummaryPage from './DatasetSummaryPage/DatasetSummaryPage';
import DatasetVersionPage from './DatasetVersionPage/DatasetVersionPage';
import DatasetVersionsPage from './DatasetVersionsPage/DatasetVersionsPage';
import { IPages } from 'pages/authorized/types';
import { withLoadDataset } from './LoadDataset';

export const datasetDetailsPages: IPages = {
  getPages: () => [
    <Route
      exact={true}
      path={routes.datasetSummary.getPath()}
      key={routes.datasetSummary.getPath()}
      component={withLoadDataset(DatasetSummaryPage)}
    />,
    <Route
      exact={true}
      path={routes.datasetVersions.getPath()}
      key={routes.datasetVersions.getPath()}
      component={withLoadDataset(DatasetVersionsPage)}
    />,
    <Route
      exact={true}
      path={routes.datasetVersion.getPath()}
      key={routes.datasetVersion.getPath()}
      component={withLoadDataset(DatasetVersionPage)}
    />,
    <Route
      exact={true}
      path={routes.compareDatasetVersions.getPath()}
      key={routes.compareDatasetVersions.getPath()}
      component={withLoadDataset(CompareDatasetVersionsPage)}
    />,
  ],
};
