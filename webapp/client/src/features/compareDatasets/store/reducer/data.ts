import * as R from 'ramda';
import { Reducer } from 'redux';

import {
  ComparedDatasetVersionIds,
  FeatureAction,
  ICompareDatasetsState,
  selectEntityForComparingActionType,
  unselectEntityForComparingActionType,
} from '../types';

const initial: ICompareDatasetsState['data'] = {
  comparedEntityIdsByContainerId: {},
};

const updateContainerComparedEntityIds = (
  f: (ids: ComparedDatasetVersionIds) => ComparedDatasetVersionIds,
  projectId: string,
  state: ICompareDatasetsState['data']
): ICompareDatasetsState['data'] => {
  return {
    ...state,
    comparedEntityIdsByContainerId: {
      ...state.comparedEntityIdsByContainerId,
      [projectId]: f(state.comparedEntityIdsByContainerId[projectId] || []),
    },
  };
};

const dataReducer: Reducer<ICompareDatasetsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING: {
      return updateContainerComparedEntityIds(
        comparedEntityIds =>
          comparedEntityIds.concat(
            action.payload.modelRecordId
          ) as ComparedDatasetVersionIds,
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
          ) as ComparedDatasetVersionIds,
        action.payload.projectId,
        state
      );
    }
    default:
      return state;
  }
};

export default dataReducer;
