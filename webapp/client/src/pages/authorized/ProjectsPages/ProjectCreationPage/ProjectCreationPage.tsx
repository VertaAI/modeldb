import React from 'react';

import { ProjectCreation } from 'features/projectCreation';
import {
  AuthorizedLayout,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';

const breadcrumbsBuilder = BreadcrumbsBuilder()
  .then({ type: 'single', route: routes.projects, getName: () => 'Projects' })
  .then({
    type: 'single',
    route: routes.projectCreation,
    getName: () => 'Project creation',
  });

const ProjectCreationPage = () => {
  return (
    <AuthorizedLayout breadcrumbsBuilder={breadcrumbsBuilder}>
      <ProjectCreation />
    </AuthorizedLayout>
  );
};

export default ProjectCreationPage;
