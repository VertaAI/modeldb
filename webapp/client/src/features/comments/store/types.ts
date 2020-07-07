import { AppError } from 'shared/models/Error';
import {
  ICommunication,
  ICommunicationById,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'shared/utils/redux/communication';

import { EntityId, IComment } from '../../../shared/models/Comment';

export interface ICommentsRootState {
  comments: ICommentsState;
}

export interface ICommentsState {
  data: {
    entitiesComments: IEntitiesComments;
  };
  communications: {
    loadingComments: ICommunicationById<EntityId>;
    addingComment: ICommunication;
    deletingComment: ICommunicationById<EntityId>;
  };
}

export type IEntitiesComments = Record<EntityId, IComment[] | undefined>;

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
export interface ISetEntitiesComments {
  type: setEntitiesCommentsActionType.SET_ENTITIES_COMMENTS;
  payload: Array<{ entityId: string; comments: IComment[] }>;
}

export type FeatureAction =
  | ILoadCommentsActions
  | IAddCommentActions
  | IDeleteCommentActions
  | IResetEntityComments
  | ISetEntitiesComments;
