import { IRepository } from 'core/shared/models/Versioning/Repository';

import { currentUser } from '../../../../../../utils/tests/mocks/models/users';
import { userWorkspacesWithCurrentUser } from '../../../../../../utils/tests/mocks/models/workspace';

export const repositories: IRepository[] = [
  {
    dateCreated: new Date(),
    dateUpdated: new Date(),
    id: 'repository-id',
    name: 'repository-name',
    shortWorkspace: userWorkspacesWithCurrentUser.user,
    labels: [],
    owner: currentUser,
  },
];
