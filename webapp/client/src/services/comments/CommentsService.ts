import { BaseDataService } from 'services/BaseDataService';

import { EntityId, IComment } from 'shared/models/Comment';
import { convertServerComment } from '../serverModel/Comments/converters';

export default class CommentsService extends BaseDataService {
  public async addComment(
    entityId: EntityId,
    message: string
  ): Promise<IComment> {
    const response = await this.post({
      url: '/v1/modeldb/comment/addExperimentRunComment',
      data: {
        message,
        entity_id: entityId,
      },
    });
    return convertServerComment(response.data.comment);
  }

  public async deleteComment(entityId: EntityId, id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/comment/deleteExperimentRunComment',
      config: { params: { id, entity_id: entityId } },
    });
  }

  public async updateComment(
    entityId: EntityId,
    id: string,
    message: string
  ): Promise<void> {
    await this.post({
      url: '/v1/modeldb/comment/updateExperimentRunComment',
      data: {
        id,
        message,
        entity_id: entityId,
      },
    });
  }

  public async loadComments(entityId: string): Promise<IComment[]> {
    return this.get({
      url: `/v1/modeldb/comment/getExperimentRunComments`,
      config: { params: { entity_id: entityId } },
    }).then(({ data }) => {
      return (data.comments || []).map((serverComment: any) => {
        return convertServerComment(serverComment);
      });
    });
  }
}
