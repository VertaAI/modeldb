import React from 'react';

import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'shared/routes';

type ILocalProps = IAuthorizedLayoutLocalProps;

const brearcrumbsBuilder = BreadcrumbsBuilder()
  .then({
    type: 'single',
    route: routes.repositories,
    getName: () => 'Repositories',
  })
  .then({
    type: 'single',
    route: routes.createRepository,
    getName: () => 'New repository',
  });

const RepositoriesPagesLayout: React.FC<ILocalProps> = ({ children }) => {
  return (
    <AuthorizedLayout breadcrumbsBuilder={brearcrumbsBuilder}>
      {children}
    </AuthorizedLayout>
  );
};

export default RepositoriesPagesLayout;
