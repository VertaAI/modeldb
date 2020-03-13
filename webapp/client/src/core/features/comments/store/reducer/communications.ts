import { combineReducers } from 'redux';

import {
  CommunicationActionsToObj,
  makeCommunicationReducerByIdFromEnum,
  makeCommunicationReducerFromEnum,
} from 'core/shared/utils/redux/communication';

import {
  addCommentActionTypes,
  deleteCommentActionTypes,
  ICommentsState,
  IDeleteCommentActions,
  ILoadCommentsActions,
  loadCommentsActionTypes,
} from '../types';

export default combineReducers<ICommentsState['communications']>({
  loadingComments: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadCommentsActions,
      typeof loadCommentsActionTypes
    >,
    string
  >(loadCommentsActionTypes, {
    request: id => id,
    success: payload => payload.entityId,
    failure: payload => payload.entityId,
  }),
  addingComment: makeCommunicationReducerFromEnum(addCommentActionTypes),
  deletingComment: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteCommentActions,
      typeof deleteCommentActionTypes
    >,
    string
  >(deleteCommentActionTypes, {
    request: ({ commentId }) => commentId,
    success: ({ commentId }) => commentId,
    failure: ({ commentId }) => commentId,
  }),
});
