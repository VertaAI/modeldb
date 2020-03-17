import * as React from 'react';
import { useParams } from 'react-router';

import { Commit } from 'core/features/repository/viewCommit';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import routes, { GetRouteParams } from 'routes';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';
import styles from './CommitPage.module.css';

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
