import { IUserWorkspaces } from 'shared/models/Workspace';

import { currentUser } from './users';

export const userWorkspacesWithCurrentUser: IUserWorkspaces = {
  user: { type: 'user', id: currentUser.id, name: 'personal' },
};
