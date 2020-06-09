import * as React from 'react';

import { CommitsHistory } from 'features/versioning/commitsHistory';
import { IRepository } from 'shared/models/Versioning/Repository';
import { PageCard } from 'shared/view/elements/PageComponents';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

const CommitsHistoryPage = ({ repository }: ILocalProps) => {
  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <PageCard>
        <CommitsHistory repository={repository} />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default CommitsHistoryPage;
