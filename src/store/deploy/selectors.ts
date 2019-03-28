import { IDeployStatusInfo } from 'models/Deploy';

import { IApplicationState } from '../store';
import { IDeployState } from './types';

const selectState = (state: IApplicationState): IDeployState => state.deploy;

export const selectDeployStatusInfo = (state: IApplicationState, modelId: string): IDeployStatusInfo => {
  return selectState(state).data[modelId] || { status: 'notDeployed' };
};

export const selectIsLoadingDeployStatusInfo = (state: IApplicationState, modelId: string): boolean => {
  const loadingModelDeployStatus = selectState(state).loadingDeployStatus[modelId];
  return Boolean(loadingModelDeployStatus && loadingModelDeployStatus.isRequesting);
};

export const selectIsCheckingDeployStatusInfo = selectIsLoadingDeployStatusInfo;
