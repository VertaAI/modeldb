import * as React from 'react';
import { useParams } from 'react-router';

import { Commit } from 'features/versioning/viewCommit';
import { IRepository } from 'shared/models/Versioning/Repository';
import routes, { GetRouteParams } from 'shared/routes';

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
