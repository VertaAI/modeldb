import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import { getCommitReference } from 'core/shared/graphql/Versioning/CommitReference';

import * as Types from './graphql-types/CompareChanges';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';

import { ICompareChangesData } from '../types';
import { convertGraphqlDiffs } from 'core/shared/graphql/Versioning/Diff';

export { Types };
export const COMPARE_CHANGES = gql`
  query CompareChanges(
    $repositoryId: ID!
    $commitAReference: CommitReference!
    $commitBReference: CommitReference!
  ) {
    repository(id: $repositoryId) {
      id
      commitA: commitByReference(ref: $commitAReference) {
        id
      }
      commitB: commitByReference(ref: $commitBReference) {
        id
      }
      diff(a: $commitAReference, b: $commitBReference)
    }
  }
`;
export const useCompareChangesMutation = ({
  repositoryId,
  commitPointerA,
  commitPointerB,
}: {
  repositoryId: IRepository['id'];
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
}) => {
  const res = useQuery<Types.CompareChanges, Types.CompareChangesVariables>(
    COMPARE_CHANGES,
    {
      variables: {
        repositoryId,
        commitAReference: getCommitReference(commitPointerA),
        commitBReference: getCommitReference(commitPointerB),
      },
    }
  );

  return resultToCommunicationWithData(serverRes => {
    if (
      serverRes.repository &&
      serverRes.repository.commitA &&
      serverRes.repository.commitB
    ) {
      const res: ICompareChangesData = {
        commits: {
          commitPointerA: {
            sha: serverRes.repository.commitA.id,
          },
          commitPointerB: {
            sha: serverRes.repository.commitB.id,
          },
        },
        diffs: convertGraphqlDiffs(serverRes.repository.diff),
      };
      return res;
    }
  }, res);
};
