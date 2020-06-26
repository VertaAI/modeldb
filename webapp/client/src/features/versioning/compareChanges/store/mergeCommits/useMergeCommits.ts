import gql from 'graphql-tag';
import * as R from 'ramda';
import { useMutation } from 'react-apollo';

import { IRepository } from 'shared/models/Versioning/Repository';
import { SHA, CommitPointer } from 'shared/models/Versioning/RepositoryData';
import onCompletedUpdate from 'shared/utils/graphql/onCompletedUpdate';
import resultToCommunicationWithData, {
  mutationResultToCommunication,
} from 'shared/utils/graphql/queryResultToCommunicationWithData';
import { initialCommunication } from 'shared/utils/redux/communication';

import * as MergeCommitsTypes from './graphql-types/MergeCommits';
import * as UpdateBaseBranchTypes from './graphql-types/UpdateBaseBranch';

export { MergeCommitsTypes, UpdateBaseBranchTypes };

export const UPDATE_BASE_BRANCH = gql`
  mutation UpdateBaseBranch(
    $repositoryId: ID!
    $commitSha: ID!
    $branch: String!
  ) {
    repository(id: $repositoryId) {
      id
      commit(id: $commitSha) {
        setBranch(name: $branch) {
          id
        }
      }
    }
  }
`;
const useUpdateBaseBranchMutation = () => {
  const [runMutation, result] = useMutation<
    UpdateBaseBranchTypes.UpdateBaseBranch,
    UpdateBaseBranchTypes.UpdateBaseBranchVariables
  >(UPDATE_BASE_BRANCH);

  return {
    updateBaseBranch: runMutation,
    communication: mutationResultToCommunication(result),
  };
};

export const MERGE_COMMITS = gql`
  mutation MergeCommits(
    $repositoryId: ID!
    $commitASha: ID!
    $commitBSha: ID!
  ) {
    repository(id: $repositoryId) {
      id
      merge(a: { commit: $commitASha }, b: { commit: $commitBSha }) {
        commit {
          id
        }
        conflicts
      }
    }
  }
`;
export const useMergeCommitsMutation = () => {
  const {
    updateBaseBranch,
    communication: updatingBaseBranch,
  } = useUpdateBaseBranchMutation();

  const [runMutation, mergeResult] = useMutation<
    MergeCommitsTypes.MergeCommits,
    MergeCommitsTypes.MergeCommitsVariables
  >(MERGE_COMMITS);
  const mergeCommits = (
    {
      repositoryId,
      commitASha,
      commitBSha,
      base,
    }: {
      repositoryId: IRepository['id'];
      commitASha: SHA;
      commitBSha: SHA;
      base: CommitPointer;
    },
    onMerged: () => void
  ) => {
    runMutation({
      variables: {
        commitASha,
        commitBSha,
        repositoryId,
      },
      update: onCompletedUpdate((data) => {
        const newCommit =
          data &&
          data.repository &&
          data.repository.merge &&
          data.repository.merge &&
          data.repository.merge.commit;
        if (newCommit) {
          if (base.type === 'branch') {
            updateBaseBranch({
              variables: {
                branch: base.value,
                commitSha: newCommit.id,
                repositoryId,
              },
              update: onCompletedUpdate(() => {
                onMerged();
              }),
            });
          } else {
            onMerged();
          }
        }
      }),
    });
  };

  return {
    mergeCommits,
    communicationWithData: R.equals(updatingBaseBranch, initialCommunication)
      ? resultToCommunicationWithData(({ repository }) => {
          return {
            isMergeConflict: Boolean(repository && repository.merge.conflicts),
          };
        }, mergeResult)
      : { communication: updatingBaseBranch, data: undefined },
  };
};
