import { action } from 'typesafe-actions';

import normalizeError from 'core/shared/utils/normalizeError';

import { EntityId, IComment } from '../Model';
import { ActionResult } from './deps';
import {
  addCommentActionTypes,
  deleteCommentActionTypes,
  IAddCommentActions,
  IDeleteCommentActions,
  ILoadCommentsActions,
  IResetEntityComments,
  loadCommentsActionTypes,
  resetEntityCommentsActionTypes,
  ISetEntitiesComments,
  setEntitiesCommentsActionType,
} from './types';

export const resetEntityComments = (
  entityId: string
): IResetEntityComments => ({
  type: resetEntityCommentsActionTypes.RESET_ENTITY_COMMENTS,
  payload: entityId,
});

export const loadComments = (
  entityId: EntityId
): ActionResult<void, ILoadCommentsActions> => async (
  dispatch,
  getState,
  { makeCommentsService }
) => {
  dispatch(action(loadCommentsActionTypes.REQUEST, entityId));

  await makeCommentsService({ dispatch, getState })
    .loadComments(entityId)
    .then(res => {
      dispatch(
        action(loadCommentsActionTypes.SUCCESS, { entityId, data: res })
      );
    })
    .catch(error => {
      dispatch(
        action(loadCommentsActionTypes.FAILURE, {
          entityId,
          error: normalizeError(error),
        })
      );
    });
};

export const addComment = (
  entityId: EntityId,
  message: string
): ActionResult<void, IAddCommentActions> => async (
  dispatch,
  getState,
  { makeCommentsService }
) => {
  dispatch(action(addCommentActionTypes.REQUEST, entityId));

  await makeCommentsService({ dispatch, getState })
    .addComment(entityId, message)
    .then(comment => {
      dispatch(action(addCommentActionTypes.SUCCESS, { entityId, comment }));
    })
    .catch(error => {
      dispatch(action(addCommentActionTypes.FAILURE, normalizeError(error)));
    });
};

export const deleteComment = (
  entityId: EntityId,
  commentId: string
): ActionResult<void, IDeleteCommentActions> => async (
  dispatch,
  getState,
  { makeCommentsService }
) => {
  dispatch(action(deleteCommentActionTypes.REQUEST, { entityId, commentId }));

  await makeCommentsService({ dispatch, getState })
    .deleteComment(entityId, commentId)
    .then(_ => {
      dispatch(
        action(deleteCommentActionTypes.SUCCESS, { entityId, commentId })
      );
    })
    .catch(error => {
      dispatch(
        action(deleteCommentActionTypes.FAILURE, {
          commentId,
          error: normalizeError(error),
        })
      );
    });
};

export const setEntitiesComments = <Comment extends IComment>(
  payload: ISetEntitiesComments<Comment>['payload']
): ISetEntitiesComments<Comment> => ({
  type: setEntitiesCommentsActionType.SET_ENTITIES_COMMENTS,
  payload,
});
