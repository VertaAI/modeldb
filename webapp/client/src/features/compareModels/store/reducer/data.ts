import * as R from 'ramda';
import { Reducer } from 'redux';

import {
  ComparedModelIds,
  FeatureAction,
  ICompareModelsState,
  selectEntityForComparingActionType,
  unselectEntityForComparingActionType,
} from '../types';

const initial: ICompareModelsState['data'] = {
  comparedEntityIdsByContainerId: {},
};

const updateContainerComparedEntityIds = (
  f: (ids: ComparedModelIds) => ComparedModelIds,
  projectId: string,
  state: ICompareModelsState['data']
): ICompareModelsState['data'] => {
  return {
    ...state,
    comparedEntityIdsByContainerId: {
      ...state.comparedEntityIdsByContainerId,
      [projectId]: f(state.comparedEntityIdsByContainerId[projectId] || []),
    },
  };
};

const dataReducer: Reducer<ICompareModelsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING: {
      return updateContainerComparedEntityIds(
        comparedEntityIds =>
          comparedEntityIds.concat(
            action.payload.modelRecordId
          ) as ComparedModelIds,
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
          ) as ComparedModelIds,
        action.payload.projectId,
        state
      );
    }
    default:
      return state;
  }
};

export default dataReducer;
