import gql from 'graphql-tag';

import User from 'core/shared/models/User';
import { Repositories_workspace_repositories_repositories_collaborators_UserCollaborator_user } from 'features/versioning/repositories/store/repositoriesQuery/graphql-types/Repositories';

export const convertUserToServer = (
  user: User
): Repositories_workspace_repositories_repositories_collaborators_UserCollaborator_user => {
  return {
    __typename: 'User',
    email: user.email,
    id: user.id,
    username: user.username,
    picture: user.picture || null,
  };
};
export const convertUser = (
  serverUser: Repositories_workspace_repositories_repositories_collaborators_UserCollaborator_user
) => {
  return new User({
    email: serverUser.email,
    id: serverUser.id,
    username: serverUser.username as User['username'],
    fullName: serverUser.username as User['fullName'],
    picture: serverUser.picture || undefined,
  });
};
export const USER_FRAGMENT = gql`
  fragment UserData on User {
    id
    email
    picture
    username
  }
`;
