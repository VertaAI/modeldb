import * as React from 'react';
import { useParams } from 'react-router';

import { CompareChanges } from 'core/features/versioning/compareChanges';
import withABCommitPointers from 'core/features/versioning/shared/withABCommitPointers';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import routes, { GetRouteParams } from 'core/shared/routes';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

const CompareChangesPage = ({ repository }: ILocalProps) => {
  const { commitPointerAValue, commitPointerBValue } = useParams<
    GetRouteParams<typeof routes.repositoryCompareChanges>
  >();

  const CompareChagesWithCommitPointers = withABCommitPointers(CompareChanges)({
    commitPointerAValue,
    commitPointerBValue,
  });

  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <div style={{ width: '100%' }}>
        <CompareChagesWithCommitPointers repository={repository} />
      </div>
    </RepositoryDetailsPagesLayout>
  );
};

export default CompareChangesPage;
