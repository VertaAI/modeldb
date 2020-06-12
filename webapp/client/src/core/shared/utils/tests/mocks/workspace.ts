import {
  IUserWorkspaces,
} from 'models/Workspace';

import { currentUser } from './users';

export const userWorkspacesWithCurrentUser: IUserWorkspaces = {
  user: { type: 'user', id: currentUser.id, name: currentUser.username },
};