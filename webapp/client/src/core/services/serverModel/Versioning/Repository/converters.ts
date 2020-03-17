import { IRepository, Label } from 'core/shared/models/Repository/Repository';
import { convertServerEntityWithLoggedDates } from 'services/serverModel/Common/converters';
import { convertServerShortWorkspaceToClient } from 'services/serverModel/Workspace/converters';

import User from 'models/User';
import { IServerRepository } from './Repository';

export const convertServerRepositoryToClient = ({
  serverRepository,
  labels,
  owner,
}: {
  serverRepository: IServerRepository;
  labels: Label[];
  owner: User;
}): IRepository => {
  const { dateCreated, dateUpdated } = convertServerEntityWithLoggedDates({
    date_created: serverRepository.date_created,
    date_updated: serverRepository.date_updated,
  });
  return {
    id: serverRepository.id,
    name: serverRepository.name,
    shortWorkspace: convertServerShortWorkspaceToClient(serverRepository),
    dateCreated,
    dateUpdated,
    labels,
    owner,
  };
};
