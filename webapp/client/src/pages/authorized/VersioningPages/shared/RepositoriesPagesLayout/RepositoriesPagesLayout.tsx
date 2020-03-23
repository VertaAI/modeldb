import React from 'react';

import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';

type ILocalProps = IAuthorizedLayoutLocalProps;

const brearcrumbsBuilder = BreadcrumbsBuilder()
  .then({
    routes: [routes.repositories],
    getName: () => 'Repositories',
  })
  .then({
    routes: [routes.createRepository],
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
