import { bind } from 'decko';

import { IRepository, Label } from 'core/shared/models/Repository/Repository';
import { BaseDataService } from '../../BaseDataService';

export default class MetaDataService extends BaseDataService {
  constructor() {
    super();
  }

  @bind
  public async loadRepositoryLabel(
    repositoryId: IRepository['id']
  ): Promise<Label[]> {
    const response = await this.get<{ labels?: Label[] }>({
      url: '/v1/modeldb/metadata/labels',
      config: {
        params: {
          'id.id_type': 1,
          'id.int_id': +repositoryId,
        },
      },
    });

    return response.data.labels || [];
  }

  @bind
  public async deleteRepositoryLabels(
    repositoryId: IRepository['id'],
    label: Label
  ): Promise<void> {
    const response = await this.delete<Label[]>({
      url: '/v1/modeldb/metadata/labels',
      config: {
        data: {
          id: {
            id_type: 1,
            int_id: +repositoryId,
          },
          labels: [label],
        },
      },
    });
  }

  @bind
  public async addRepositoryLabels(
    repositoryId: IRepository['id'],
    label: Label
  ): Promise<void> {
    const response = await this.put<Label[]>({
      url: '/v1/modeldb/metadata/labels',
      data: {
        id: {
          id_type: 1,
          int_id: +repositoryId,
        },
        labels: [label],
      },
    });
  }
}
