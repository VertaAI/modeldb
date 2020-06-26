import gql from 'graphql-tag';
import { useMutation } from 'react-apollo';

import { getCommitReference } from 'shared/graphql/Versioning/CommitReference';
import { convertGraphqlDiffs } from 'shared/graphql/Versioning/Diff';
import { IRepository } from 'shared/models/Versioning/Repository';
import { CommitPointer } from 'shared/models/Versioning/RepositoryData';
import resultToCommunicationWithData from 'shared/utils/graphql/queryResultToCommunicationWithData';

import { ICompareChangesData } from '../types';
import * as Types from './graphql-types/CompareChanges';
import { useEffect } from 'react';

export { Types };
export const COMPARE_CHANGES = gql`
  mutation CompareChanges(
    $repositoryId: ID!
    $commitReferenceA: CommitReference!
    $commitReferenceB: CommitReference!
  ) {
    repository(id: $repositoryId) {
      id
      commitA: commitByReference(ref: $commitReferenceA) {
        id
      }
      commitB: commitByReference(ref: $commitReferenceB) {
        id
      }
      diff(a: $commitReferenceA, b: $commitReferenceB)
      merge(a: $commitReferenceA, b: $commitReferenceB, isDryRun: true) {
        conflicts
      }
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
  const [runMutation, result] = useMutation<
    Types.CompareChanges,
    Types.CompareChangesVariables
  >(COMPARE_CHANGES);

  useEffect(() => {
    runMutation({
      variables: {
        repositoryId,
        commitReferenceA: getCommitReference(commitPointerA),
        commitReferenceB: getCommitReference(commitPointerB),
      },
    });
  }, [repositoryId, commitPointerA, commitPointerB]);

  return resultToCommunicationWithData((serverRes) => {
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
        isMergeConflict: Boolean(
          serverRes.repository.merge.conflicts &&
            serverRes.repository.merge.conflicts.length > 0
        ),
      };
      return res;
    }
  }, result);
};
