import React from 'react';

import DatasetCreation from 'features/datasets/view/DatasetCreation/DatasetCreation';
import {
  AuthorizedLayout,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'core/shared/routes';

class DatasetCreationPage extends React.Component {
  private breadcrumbsBuilder = BreadcrumbsBuilder()
    .then({ type: 'single', route: routes.datasets, getName: () => 'Datasets' })
    .then({
      type: 'single',
      route: routes.datasetCreation,
      getName: () => 'Dataset creation',
    });

  public render() {
    return (
      <AuthorizedLayout breadcrumbsBuilder={this.breadcrumbsBuilder}>
        <DatasetCreation />
      </AuthorizedLayout>
    );
  }
}

export default DatasetCreationPage;
