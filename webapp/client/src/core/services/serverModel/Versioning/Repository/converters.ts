import { IRepository, Label } from 'core/shared/models/Versioning/Repository';
import { convertServerEntityWithLoggedDates } from 'services/serverModel/Common/converters';
import { convertServerShortWorkspaceToClient } from 'services/serverModel/Workspace/converters';

import { IServerRepository } from './Repository';
import matchType from 'core/shared/utils/matchType';
import User from 'models/User';

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
    visibility: serverRepository.repository_visibility
      ? matchType<
          Required<IServerRepository>['repository_visibility'],
          IRepository['visibility']
        >(
          {
            ORG_SCOPED_PUBLIC: () => 'organizationPublic',
            PRIVATE: () => 'private',
            PUBLIC: () => 'public',
          },
          serverRepository.repository_visibility
        )
      : 'private',
  }
};
