import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';

import { IPages } from '../types';
import DatasetsPage from './DatasetsPage/DatasetsPage';
import DatasetCreationPage from './DatasetCreationPage/DatasetCreationPage';
import { datasetDetailsPages } from './DatasetDetailPages';

export const datasetPages: IPages = {
  getPages: () => [
    <Route
      key={routes.datasets.getPath()}
      path={routes.datasets.getPath()}
      exact={true}
      component={DatasetsPage}
    />,
    <Route
      key={routes.datasetCreation.getPath()}
      path={routes.datasetCreation.getPath()}
      exact={true}
      component={DatasetCreationPage}
    />,
    ...datasetDetailsPages.getPages(),
  ],
};
