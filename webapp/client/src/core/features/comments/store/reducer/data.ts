import * as R from 'ramda';
import { Reducer } from 'redux';

import { EntityId, IComment } from 'core/features/comments/Model';

import {
  addCommentActionTypes,
  deleteCommentActionTypes,
  FeatureAction,
  ICommentsState,
  loadCommentsActionTypes,
  resetEntityCommentsActionTypes,
  setEntitiesCommentsActionType,
} from '../types';

const initial: ICommentsState['data'] = {
  entitiesComments: {},
};

const updateComments = (
  f: (x: IComment[] | undefined) => IComment[] | undefined,
  id: EntityId,
  state: ICommentsState['data']
): ICommentsState['data'] => {
  return {
    ...state,
    entitiesComments: {
      ...state.entitiesComments,
      [id]: f(state.entitiesComments[id]),
    },
  };
};

const dataReducer: Reducer<ICommentsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadCommentsActionTypes.SUCCESS: {
      return updateComments(
        () => action.payload.data,
        action.payload.entityId,
        state
      );
    }
    case addCommentActionTypes.SUCCESS: {
      return updateComments(
        comments => (comments || []).concat(action.payload.comment),
        action.payload.entityId,
        state
      );
    }
    case deleteCommentActionTypes.SUCCESS: {
      return updateComments(
        comments =>
          (comments || []).filter(
            comment => comment.id !== action.payload.commentId
          ),
        action.payload.entityId,
        state
      );
    }
    case resetEntityCommentsActionTypes.RESET_ENTITY_COMMENTS: {
      return updateComments(_ => undefined, action.payload, state);
    }
    case setEntitiesCommentsActionType.SET_ENTITIES_COMMENTS: {
      const newEntitiesComments = R.fromPairs(
        action.payload.map(({ entityId, comments }) => [entityId, comments])
      );
      return {
        ...state,
        entitiesComments: {
          ...state.entitiesComments,
          ...newEntitiesComments,
        },
      };
    }
    default:
      return state;
  }
};

export default dataReducer;
