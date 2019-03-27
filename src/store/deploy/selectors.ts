import { IDeployInfo } from 'models/Deploy';

import { IApplicationState } from '../store';
import { IDeployState } from './types';

const selectState = (state: IApplicationState): IDeployState => state.deploy;

export function selectDeployInfo(state: IApplicationState, modelId: string): IDeployInfo {
  const deployResult = selectState(state).data[modelId];
  const isDeploying = selectIsDeploying(state, modelId);
  if (deployResult) {
    return { status: 'running', result: deployResult };
  }
  if (isDeploying) {
    return { status: 'building' };
  }
  return { status: 'not-deployed' };
}

export function selectIsDeploying(state: IApplicationState, modelId: string): boolean {
  const modelDeploying = selectState(state).deploying[modelId];
  return Boolean(modelDeploying && modelDeploying.isRequesting);
}
