import gql from 'graphql-tag';
import { useMutation } from 'react-apollo';

import { getCommitReference } from 'core/shared/graphql/Versioning/CommitReference';
import { convertGraphqlDiffs } from 'core/shared/graphql/Versioning/Diff';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';

import * as MergeConflictsTypes from './graphql-types/MergeConflicts';

export { MergeConflictsTypes };

export const MERGE_CONFLICTS = gql`
  mutation MergeConflicts(
    $repositoryId: ID!
    $commitReferenceA: CommitReference!
    $commitReferenceB: CommitReference!
    $isDryRun: Boolean!
  ) {
    repository(id: $repositoryId) {
      id
      commitA: commitByReference(ref: $commitReferenceA) {
        id
        sha: id
      }
      commitB: commitByReference(ref: $commitReferenceB) {
        id
        sha: id
      }
      merge(a: $commitReferenceA, b: $commitReferenceB, isDryRun: $isDryRun) {
        commonBase {
          id
          sha: id
        }
        conflicts
      }
    }
  }
`;

export const useMergeConflictsMutation = () => {
  const [runMutation, result] = useMutation<
    MergeConflictsTypes.MergeConflicts,
    MergeConflictsTypes.MergeConflictsVariables
  >(MERGE_CONFLICTS);

  const fetchMergeConflicts = ({
    repositoryId,
    commitPointerA,
    commitPointerB,
  }: {
    repositoryId: IRepository['id'];
    commitPointerA: CommitPointer;
    commitPointerB: CommitPointer;
  }) =>
    runMutation({
      variables: {
        repositoryId,
        commitReferenceA: getCommitReference(commitPointerA),
        commitReferenceB: getCommitReference(commitPointerB),
        isDryRun: true,
      },
    });

  return {
    fetchMergeConflicts,
    communicationWithData: resultToCommunicationWithData(({ repository }) => {
      if (repository && repository.commitA && repository.commitB) {
        return {
          isMergeConflict: Boolean(
            repository.merge.conflicts && repository.merge.conflicts.length > 0
          ),
          conflicts: convertGraphqlDiffs(repository.merge.conflicts),
          commonBaseCommit: repository.merge.commonBase,
          commitA: repository.commitA,
          commitB: repository.commitB,
        };
      }
    }, result),
  };
};
