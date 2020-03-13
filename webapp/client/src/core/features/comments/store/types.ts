import { AppError } from 'core/shared/models/Error';
import {
  ICommunication,
  ICommunicationById,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';

import { EntityId, IComment } from '../Model';

export interface ICommentsRootState<Comment extends IComment = IComment> {
  comments: ICommentsState<Comment>;
}

export interface ICommentsState<Comment extends IComment = IComment> {
  data: {
    entitiesComments: IEntitiesComments<Comment>;
  };
  communications: {
    loadingComments: ICommunicationById<EntityId>;
    addingComment: ICommunication;
    deletingComment: ICommunicationById<EntityId>;
  };
}

export type IEntitiesComments<Comment extends IComment = IComment> = Record<
  EntityId,
  Comment[] | undefined
>;

export const loadCommentsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@comments/LOAD_COMMENTS_REQUEST',
  SUCCESS: '@@comments/LOAD_COMMENTS_SUCСESS',
  FAILURE: '@@comments/LOAD_COMMENTS_FAILURE',
});
export type ILoadCommentsActions = MakeCommunicationActions<
  typeof loadCommentsActionTypes,
  {
    request: EntityId;
    success: { entityId: EntityId; data: IComment[] };
    failure: { entityId: EntityId; error: AppError };
  }
>;

export const addCommentActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@comments/ADD_COMMENT_REQUEST',
  SUCCESS: '@@comments/ADD_COMMENT_SUCСESS',
  FAILURE: '@@comments/ADD_COMMENT_FAILURE',
});
export type IAddCommentActions = MakeCommunicationActions<
  typeof addCommentActionTypes,
  { request: EntityId; success: { entityId: EntityId; comment: IComment } }
>;

export const deleteCommentActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@comments/DELETE_COMMENT_REQUEST',
  SUCCESS: '@@comments/DELETE_COMMENT_SUCСESS',
  FAILURE: '@@comments/DELETE_COMMENT_FAILURE',
});
export type IDeleteCommentActions = MakeCommunicationActions<
  typeof deleteCommentActionTypes,
  {
    request: { entityId: EntityId; commentId: string };
    success: { entityId: string; commentId: string };
    failure: { commentId: string; error: AppError };
  }
>;

export enum resetEntityCommentsActionTypes {
  RESET_ENTITY_COMMENTS = '@@comments/RESET_ENTITY_COMMENTS',
}
export interface IResetEntityComments {
  type: resetEntityCommentsActionTypes.RESET_ENTITY_COMMENTS;
  payload: EntityId;
}

export enum setEntitiesCommentsActionType {
  SET_ENTITIES_COMMENTS = '@@comments/SET_ENTITIES_COMMENTS',
}
export interface ISetEntitiesComments<Comment extends IComment> {
  type: setEntitiesCommentsActionType.SET_ENTITIES_COMMENTS;
  payload: Array<{ entityId: string; comments: Comment[] }>;
}

export type FeatureAction =
  | ILoadCommentsActions
  | IAddCommentActions
  | IDeleteCommentActions
  | IResetEntityComments
  | ISetEntitiesComments<IComment>;
