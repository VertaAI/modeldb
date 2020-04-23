import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';
import { IPages } from 'pages/authorized/types';

import RepositoryDetailsPages from './RepositoryDetailsPages';

export const repositoryDetailsPages: IPages = {
  getPages: () => [
    <Route
      key={routes.repository.getPath()}
      path={routes.repository.getPath()}
      component={RepositoryDetailsPages}
    />,
  ],
};
