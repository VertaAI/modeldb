import { IRepository } from 'core/shared/models/Versioning/Repository';
import { ICurrentWorkspace } from 'core/shared/models/Workspace';

import { Repository_workspace_repository } from '../repositoryQuery/graphql-types/Repository';
import { convertUser } from 'core/shared/graphql/User/User';

export const convertRepository = (
  serverRepository: Repository_workspace_repository,
  currentWorkspace: ICurrentWorkspace
) => {
  const repository: IRepository = {
    dateCreated: new Date(Number(serverRepository.dateCreated)),
    dateUpdated: new Date(Number(serverRepository.dateUpdated)),
    id: serverRepository.id,
    labels: serverRepository.labels,
    name: serverRepository.name,
    owner: convertUser(serverRepository.owner),
    shortWorkspace: currentWorkspace as IRepository['shortWorkspace'],
  };
  return repository;
};
