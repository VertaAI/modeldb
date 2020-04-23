import * as React from 'react';
import { Route } from 'react-router-dom';

import routes from 'routes';

import { IPages } from '../types';
import ProjectsPage from './ProjectsPage/ProjectsPage';
import ProjectCreationPage from './ProjectCreationPage/ProjectCreationPage';
import { projectDetailsPages } from './ProjectDetailsPages';

export const projectsPages: IPages = {
  getPages: () => [
    <Route
      key={routes.projects.getPath()}
      path={routes.projects.getPath()}
      exact={true}
      component={ProjectsPage}
    />,
    <Route
      key={routes.projectCreation.getPath()}
      path={routes.projectCreation.getPath()}
      exact={true}
      component={ProjectCreationPage}
    />,
    ...projectDetailsPages.getPages(),
  ],
};
