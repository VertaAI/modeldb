import * as R from 'ramda';
import { Reducer } from 'redux';

import {
  ComparedEntityIds,
  FeatureAction,
  ICompareEntitiesState,
  selectEntityForComparingActionType,
  unselectEntityForComparingActionType,
} from '../types';

const initial: ICompareEntitiesState['data'] = {
  comparedEntityIdsByContainerId: {},
};

const updateContainerComparedEntityIds = (
  f: (ids: ComparedEntityIds) => ComparedEntityIds,
  projectId: string,
  state: ICompareEntitiesState['data']
): ICompareEntitiesState['data'] => {
  return {
    ...state,
    comparedEntityIdsByContainerId: {
      ...state.comparedEntityIdsByContainerId,
      [projectId]: f(state.comparedEntityIdsByContainerId[projectId] || []),
    },
  };
};

const dataReducer: Reducer<ICompareEntitiesState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING: {
      return updateContainerComparedEntityIds(
        comparedEntityIds =>
          comparedEntityIds.concat(
            action.payload.modelRecordId
          ) as ComparedEntityIds,
        action.payload.projectId,
        state
      );
    }
    case unselectEntityForComparingActionType.UNSELECT_ENTITY_FOR_COMPARING: {
      return updateContainerComparedEntityIds(
        comparedEntityIds =>
          R.without(
            [action.payload.modelRecordId],
            comparedEntityIds
          ) as ComparedEntityIds,
        action.payload.projectId,
        state
      );
    }
    default:
      return state;
  }
};

export default dataReducer;
