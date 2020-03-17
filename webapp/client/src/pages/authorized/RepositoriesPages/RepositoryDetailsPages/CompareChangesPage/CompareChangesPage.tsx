import * as React from 'react';
import { useParams } from 'react-router';

import { CompareChanges } from 'core/features/compareChanges';
import withLoadingRequiredData from 'core/features/repositoryData/view/RepositoryData/WithLoadingRequiredData/WithLoadingRequiredData';
import { IRepository } from 'core/shared/models/Repository/Repository';
import routes, { GetRouteParams } from 'routes';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

const CompareChangesWithBranchesAndTags = withLoadingRequiredData(
  CompareChanges
);

const CompareChangesPage = ({ repository }: ILocalProps) => {
  const params = useParams<
    GetRouteParams<typeof routes.repositoryCompareChanges>
  >();

  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <div style={{ width: '100%' }}>
        <CompareChangesWithBranchesAndTags
          repository={repository}
          commitPointerValueA={params.commitPointerAValue}
          commitPointerValueB={params.commitPointerBValue}
        />
      </div>
    </RepositoryDetailsPagesLayout>
  );
};

export default CompareChangesPage;
