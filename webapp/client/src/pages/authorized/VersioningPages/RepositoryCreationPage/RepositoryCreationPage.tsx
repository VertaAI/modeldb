import React from 'react';

import { RepositoryCreationForm } from 'core/features/versioning/repositories/view';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';

import RepositoriesPagesLayout from '../shared/RepositoriesPagesLayout/RepositoriesPagesLayout';

const RepositoryCreationPage: React.FC = () => {
  return (
    <RepositoriesPagesLayout>
      <PageCard>
        <PageHeader title="Create a new repository" />
        <RepositoryCreationForm />
      </PageCard>
    </RepositoriesPagesLayout>
  );
};

export default React.memo(RepositoryCreationPage);
