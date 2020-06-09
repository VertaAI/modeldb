import * as React from 'react';
import { useParams } from 'react-router';

import { Commit } from 'core/features/versioning/viewCommit';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import routes, { GetRouteParams } from 'core/shared/routes';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

const CommitPage = ({ repository }: ILocalProps) => {
  const params = useParams<GetRouteParams<typeof routes.repositoryCommit>>();

  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <Commit repository={repository} commitSha={params.commitSha} />
    </RepositoryDetailsPagesLayout>
  );
};

export default CommitPage;
