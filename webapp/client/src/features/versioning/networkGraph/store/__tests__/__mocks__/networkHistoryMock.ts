import { NetworkEdgeType } from 'graphql-types/graphql-global-types';

import { Network_workspace_repository_network } from '../../graphql-types/Network';

export const networkHistoryMock: Network_workspace_repository_network = {
  __typename: 'BranchesNetwork',
  commits: [
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: 'ee4ce00cfc5406da3934a347df9c045bd75371e8eb1bd5fb68ee3fa30303bd76',
      },
      color: 0,
    },
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: 'c44b83ff69b1eb794cb1eedae59d9d74dc4db8afac8a45abc9f93bda324d45e9',
      },
      color: 0,
    },
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: 'eeca7178617b8f8c7d8f6584745dbdc965dfa0141228e03c5f442b98dedd0150',
      },
      color: 1,
    },
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: '846ecb717649ce258b37370b529e51a8616263dc0e8356fa62c59b83bb35e9ad',
      },
      color: 0,
    },
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: 'b3516e6fe3451684abb4b3b8cf488e74673de09ef7ebd7a9fe512e7d6badf15e',
      },
      color: 2,
    },
    {
      __typename: 'NetworkCommitColor',
      commit: {
        __typename: 'Commit',
        message: 'Test message',
        author: {
          __typename: 'User',
          picture: null,
        },
        sha: 'b3516e6fe3451684abb4b3b8cf488e74673de09ef7ebd7a9fe512e7d6badf15e',
      },
      color: 0,
    },
  ],
  branches: [
    {
      __typename: 'NetworkBranchColor',
      branch: 'b1',
      color: 0,
      commitIndex: 5,
    },
    {
      __typename: 'NetworkBranchColor',
      branch: 'b2',
      color: 1,
      commitIndex: 2,
    },
    {
      __typename: 'NetworkBranchColor',
      branch: 'b3',
      color: 2,
      commitIndex: 4,
    },
  ],
  edges: [
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 0,
      toCommitIndex: 1,
      color: 0,
      edgeType: NetworkEdgeType.DEFAULT,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 1,
      toCommitIndex: 2,
      color: 1,
      edgeType: NetworkEdgeType.BRANCH,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 2,
      toCommitIndex: 3,
      color: 1,
      edgeType: NetworkEdgeType.MERGE,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 2,
      toCommitIndex: 4,
      color: 2,
      edgeType: NetworkEdgeType.BRANCH,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 4,
      toCommitIndex: 5,
      color: 2,
      edgeType: NetworkEdgeType.MERGE,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 1,
      toCommitIndex: 3,
      color: 0,
      edgeType: NetworkEdgeType.DEFAULT,
    },
    {
      __typename: 'NetworkEdgeColor',
      fromCommitIndex: 3,
      toCommitIndex: 5,
      color: 0,
      edgeType: NetworkEdgeType.DEFAULT,
    },
  ],
};
