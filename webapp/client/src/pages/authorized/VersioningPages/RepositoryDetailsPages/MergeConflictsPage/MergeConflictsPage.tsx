import React from 'react';
import { useParams } from 'react-router';

import { MergeConflicts } from 'features/versioning/compareChanges';
import withABCommitPointers from 'features/versioning/shared/withABCommitPointers';
import { IRepository } from 'shared/models/Versioning/Repository';
import routes, { GetRouteParams } from 'shared/routes';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

const MergeConflictsPage: React.FC<ILocalProps> = ({ repository }) => {
  const { commitPointerAValue, commitPointerBValue } = useParams<
    GetRouteParams<typeof routes.repositoryMergeConflicts>
  >();

  const MergeConflictsWithCommitPointers = withABCommitPointers(MergeConflicts)(
    {
      commitPointerAValue,
      commitPointerBValue,
    }
  );

  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <div style={{ width: '100%' }}>
        <MergeConflictsWithCommitPointers repository={repository} />
      </div>
    </RepositoryDetailsPagesLayout>
  );
};

export default MergeConflictsPage;
