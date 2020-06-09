import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IWorkspace } from 'core/shared/models/Workspace';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';
import { USER_FRAGMENT } from 'core/shared/graphql/User/User';
import { WORKSPACE_FRAGMENT } from 'core/shared/graphql/Workspace';

import * as Types from './graphql-types/Repository';
import { convertRepository } from '../converters/convertRepository';

export const REPOSITORY = gql`
  query Repository($workspaceName: String!, $repositoryName: String!) {
    workspace(name: $workspaceName) {
      ...WorkspaceData
      repository(name: $repositoryName) {
        name
        id
        dateCreated
        dateUpdated
        labels
        owner {
          ...UserData
        }
        collaborators {
          __typename
          ... on UserCollaborator {
            user {
              ...UserData
            }
            type
            canDeploy
          }
          ... on TeamCollaborator {
            team {
              id
              name
            }
            type
            canDeploy
          }
        }
        allowedActions {
          create
          update
          delete
        }
      }
    }
  }
  ${USER_FRAGMENT}
  ${WORKSPACE_FRAGMENT}
`;

export const useRepositoryQuery = ({
  name,
  workspace,
}: {
  name: IRepository['name'];
  workspace: IWorkspace;
}) => {
  const res = useQuery<Types.Repository, Types.RepositoryVariables>(
    REPOSITORY,
    {
      variables: { repositoryName: name, workspaceName: workspace.name },
    }
  );
  const communicationWithData = resultToCommunicationWithData(
    data => convertResponse(data, workspace),
    res
  );

  return {
    ...communicationWithData,
  };
};

const convertResponse = (
  res: Types.Repository | undefined,
  currentWorkspace: IWorkspace
): IRepository | undefined => {
  if (res && res.workspace && res.workspace.repository) {
    return convertRepository(res.workspace.repository, currentWorkspace);
  }
  return undefined;
};
