import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';

import { IApplicationState } from '../store';
import { IDeployState } from './types';

const selectState = (state: IApplicationState): IDeployState => state.deploy;

export const selectDeployStatusInfo = (
  state: IApplicationState,
  modelId: string
): IDeployStatusInfo => {
  return (
    selectState(state).data.deployStatusInfoByModelId[modelId] || {
      status: 'unknown',
    }
  );
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
  const loadingModelDeployStatus = selectState(state).communications
    .loadingDeployStatus[modelId];
  return Boolean(
    loadingModelDeployStatus && loadingModelDeployStatus.isRequesting
  );
};

export const selectIsLoadingDataStatistics = (
  state: IApplicationState
): boolean => {
  const loadingDataStatistics = selectState(state).communications
    .loadingDataStatistics;
  return Boolean(loadingDataStatistics && loadingDataStatistics.isRequesting);
};

export const selectIsLoadingServiceStatistics = (
  state: IApplicationState
): boolean => {
  const loadingServiceStatistics = selectState(state).communications
    .loadingServiceStatistics;
  return Boolean(
    loadingServiceStatistics && loadingServiceStatistics.isRequesting
  );
};

export const selectServiceStatistics = (
  state: IApplicationState
): IServiceStatistics | null => {
  const serviceStatistics = selectState(state).data.serviceStatistics;
  return serviceStatistics;
};

export const selectDataStatistics = (
  state: IApplicationState
): IDataStatistics | null => {
  const dataStatistics = selectState(state).data.dataStatistics;
  return dataStatistics;
};

export const selectModelId = (state: IApplicationState) => {
  return selectState(state).data.shownDeployManagerModelId;
};

export const selectIsCheckingDeployStatusInfo = selectIsLoadingDeployStatusInfo;
