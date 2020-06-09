import { IHydratedCommit } from 'core/shared/models/Versioning/RepositoryData';
import { users } from 'core/shared/utils/tests/mocks/models/users';

const commitA: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'message',
  sha: 'commit-sha',
  type: 'withParent',
};

const commitB: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'messageaa',
  sha: 'commit-shaaaa',
  type: 'withParent',
};

const commitC: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'fafaf',
  sha: 'commit-ccccc',
  type: 'withParent',
};

export const commits: [IHydratedCommit, IHydratedCommit, IHydratedCommit] = [
  commitA,
  commitB,
  commitC,
];
