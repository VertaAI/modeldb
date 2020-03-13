import { BaseDataService } from 'core/services/BaseDataService';

import { EntityId, IComment } from '../Model';
import { convertServerComment } from './serverModel/Comments/converters';

export type ICommentsService<Comment extends IComment> = Omit<
  CommentsService<Comment>,
  keyof BaseDataService
>;

export default class CommentsService<
  Comment extends IComment
> extends BaseDataService {
  constructor() {
    super();
  }

  public async addComment(
    entityId: EntityId,
    message: string
  ): Promise<Comment> {
    const response = await this.post({
      url: '/v1/modeldb/comment/addExperimentRunComment',
      data: {
        message,
        entity_id: entityId,
      },
    });
    return convertServerComment(response.data.comment) as Comment;
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

  public async loadComments(entityId: string): Promise<Comment[]> {
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
