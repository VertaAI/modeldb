import { IDeployStatusInfo } from 'models/Deploy';

import { IApplicationState } from '../store';
import { IDeployState } from './types';

const selectState = (state: IApplicationState): IDeployState => state.deploy;

export const selectDeployStatusInfo = (
  state: IApplicationState,
  modelId: string
): IDeployStatusInfo => {
  return selectState(state).data[modelId] || { status: 'unknown' };
};

export const needCheckDeployStatus = (
  state: IApplicationState,
  modelId: string
) => {
  const deployStatusInfo = selectDeployStatusInfo(state, modelId);
  const isLoadingDeployStatusInfo = selectIsLoadingDeployStatusInfo(
    state,
    modelId
  );
  return deployStatusInfo.status === 'unknown' && !isLoadingDeployStatusInfo;
};

export const selectIsLoadingDeployStatusInfo = (
  state: IApplicationState,
  modelId: string
): boolean => {
  const loadingModelDeployStatus = selectState(state).loadingDeployStatus[
    modelId
  ];
  return Boolean(
    loadingModelDeployStatus && loadingModelDeployStatus.isRequesting
  );
};

export const selectModelId = (state: IApplicationState) => {
  return selectState(state).showWizardForModel;
};

export const selectIsCheckingDeployStatusInfo = selectIsLoadingDeployStatusInfo;
