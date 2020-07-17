import { IApplicationState } from 'setup/store/store';

import {
  ICompareModelsState,
} from './types';
import { checkIsEnableModelsComparing } from './compareModels/compareModels';

const selectState = (state: IApplicationState): ICompareModelsState =>
  state.compareModels;

export const selectComparedEntityIds = (
  state: IApplicationState,
  projectId: string
) => selectState(state).data.comparedEntityIdsByContainerId[projectId] || [];

export const selectIsEnableEntitiesComparing = (
  state: IApplicationState,
  projectId: string
) => checkIsEnableModelsComparing(selectComparedEntityIds(state, projectId).length);

export const selectIsComparedEntity = (
  state: IApplicationState,
  projectId: string,
  modelId: string
) => selectComparedEntityIds(state, projectId).includes(modelId);
