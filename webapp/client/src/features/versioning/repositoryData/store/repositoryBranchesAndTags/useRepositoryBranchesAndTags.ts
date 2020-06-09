import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import {
  IRepository,
  IBranchesAndTags,
} from 'shared/models/Versioning/Repository';
import resultToCommunicationWithData from 'shared/utils/graphql/queryResultToCommunicationWithData';

import * as Types from './graphql-types/RepositoryBranchesAndTags';

export { Types };
export const REPOSITORY_BRANCHES_AND_TAGS = gql`
  query RepositoryBranchesAndTags($repositoryId: ID!) {
    repository(id: $repositoryId) {
      id
      tags {
        name
      }
      branches {
        name
      }
    }
  }
`;

export const useRepositoryBranchesAndTagsQuery = ({
  repositoryId,
}: {
  repositoryId: IRepository['id'];
}) => {
  const res = useQuery<
    Types.RepositoryBranchesAndTags,
    Types.RepositoryBranchesAndTagsVariables
  >(REPOSITORY_BRANCHES_AND_TAGS, {
    variables: { repositoryId },
  });
  const communicationWithData = resultToCommunicationWithData(data => {
    if (data.repository) {
      const res: IBranchesAndTags = {
        branches: data.repository.branches.map(({ name }) => name),
        tags: data.repository.tags.map(({ name }) => name),
      };
      return res;
    }
  }, res);

  return {
    ...communicationWithData,
  };
};
