import { EntityId } from 'core/features/comments/Model';

import { ICommentsState, ICommentsRootState } from './types';

const selectState = (state: ICommentsRootState): ICommentsState =>
  state.comments;

const selectCommunications = (state: ICommentsRootState) =>
  selectState(state).communications;

export const selectIsLoadingEntityComments = (
  state: ICommentsRootState,
  entityId: EntityId
) => {
  const comm = selectCommunications(state).loadingComments[entityId];
  return Boolean(comm && comm.isRequesting);
};

export const selectEntityComments = (
  state: ICommentsRootState,
  entityId: EntityId
) => {
  return state.comments.data.entitiesComments[entityId];
};

export const selectIsAddedComment = (state: ICommentsRootState) => {
  return selectCommunications(state).addingComment.isSuccess;
};

export const selectIsAddingComment = (state: ICommentsRootState) => {
  return selectCommunications(state).addingComment.isRequesting;
};

export const selectIsDeletingComment = (
  state: ICommentsRootState,
  commentId: string
) => {
  const comm = selectCommunications(state).deletingComment[commentId];
  return Boolean(comm && comm.isRequesting);
};
