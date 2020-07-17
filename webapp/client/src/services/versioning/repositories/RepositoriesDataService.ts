import { bind } from 'decko';

import { BaseDataService } from 'services/BaseDataService';
import { IServerRepository } from 'services/serverModel/Versioning/Repository/Repository';
import { IRepository } from 'shared/models/Versioning/Repository';

export default class RepositoriesDataService extends BaseDataService {
  @bind
  public async loadRepositoryName(
    id: IRepository['id']
  ): Promise<IRepository['name']> {
    const response = await this.get<{ repository: IServerRepository }>({
      url: `/v1/modeldb/versioning/repositories/${id}`,
    });
    const serverRepository = response.data.repository;
    return serverRepository.name;
  }
}
