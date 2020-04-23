import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';

import { IPages } from '../types';
import RepositoriesPage from './RepositoriesPage/RepositoriesPage';
import RepositoryCreationPage from './RepositoryCreationPage/RepositoryCreationPage';
import { repositoryDetailsPages } from './RepositoryDetailsPages';

export const versioningPages: IPages = {
  getPages: () => [
    <Route
      key={routes.repositories.getPath()}
      path={routes.repositories.getPath()}
      exact={true}
      component={RepositoriesPage}
    />,
    <Route
      key={routes.createRepository.getPath()}
      path={routes.createRepository.getPath()}
      exact={true}
      component={RepositoryCreationPage}
    />,
    ...repositoryDetailsPages.getPages(),
  ],
};
