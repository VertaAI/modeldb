import gql from 'graphql-tag';
import { useMutation } from 'react-apollo';
import { Visibility } from 'graphql-types/graphql-global-types';

import { mutationResultToCommunication } from 'core/shared/utils/graphql/queryResultToCommunicationWithData';
import { WORKSPACE_FRAGMENT } from 'core/shared/graphql/Workspace';

import * as Types from './graphql-types/CreateRepository';
import onCompletedUpdate from 'core/shared/utils/graphql/onCompletedUpdate';

const CREATE_REPOSITORY = gql`
  mutation CreateRepository(
    $workspaceName: String!
    $name: String!
    $visibility: Visibility!
  ) {
    workspace(name: $workspaceName) {
      ...WorkspaceData
      createRepository(name: $name, visibility: $visibility) {
        id
      }
    }
  }
  ${WORKSPACE_FRAGMENT}
`;
export const useCreateRepositoryMutation = () => {
  const [runMutation, communication] = useMutation<
    Types.CreateRepository,
    Types.CreateRepositoryVariables
  >(CREATE_REPOSITORY);
  const createRepository = (
    variables: Omit<Types.CreateRepositoryVariables, 'visibility'> & {
      isOrgPublic: boolean;
    },
    onDeleted: () => void
  ) => {
    runMutation({
      variables: {
        ...variables,
        visibility: variables.isOrgPublic
          ? Visibility.ORG_SCOPED_PUBLIC
          : Visibility.PRIVATE,
      },
      update: onCompletedUpdate(() => {
        onDeleted();
      }),
    });
  };

  return {
    createRepository,
    communication: mutationResultToCommunication(communication),
  };
};
