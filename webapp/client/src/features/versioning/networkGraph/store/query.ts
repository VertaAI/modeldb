import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { WORKSPACE_FRAGMENT } from 'core/shared/graphql/Workspace';
import { IWorkspace } from 'core/shared/models/Workspace';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';

import { convertNetwork } from './converters';
import * as ServerTypes from './graphql-types/Network';

const NETWORK = gql`
  query Network($workspaceName: String!, $repositoryName: String!) {
    workspace(name: $workspaceName) {
      ...WorkspaceData
      repository(name: $repositoryName) {
        id
        network {
          commits {
            commit {
              message
              author {
                picture
              }
              sha: id
            }
            color
          }
          branches {
            branch
            color
            commitIndex
          }
          edges {
            fromCommitIndex
            toCommitIndex
            color
            edgeType
          }
        }
      }
    }
  }
  ${WORKSPACE_FRAGMENT}
`;

export const useNetworkQuery = ({
  repositoryName,
  workspaceName,
}: {
  workspaceName: IWorkspace['name'];
  repositoryName: IRepository['name'];
}) => {
  const query = useQuery<ServerTypes.Network, ServerTypes.NetworkVariables>(
    NETWORK,
    {
      variables: {
        repositoryName,
        workspaceName,
      },
    }
  );

  const communicationWithData = resultToCommunicationWithData(
    data => convertResponse(data),
    query
  );

  return communicationWithData;
};

const convertResponse = (res: ServerTypes.Network | undefined) => {
  if (res && res.workspace && res.workspace.repository) {
    return convertNetwork(res.workspace.repository.network);
  }
};
