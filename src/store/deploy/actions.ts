import { action } from 'typesafe-actions';

import { ActionResult } from 'store/store';
import ServiceFactory from 'services/ServiceFactory';
import { UserAccess } from 'models/Project';
import { IDeployConfig, IDeployStatusInfo } from 'models/Deploy';
import { IDeployRequest } from 'services/DeployService';

import { selectIsCheckingDeployStatusInfo, selectDeployStatusInfo, selectIsLoadingDeployStatusInfo } from './selectors';
import {
  deployAction,
  deployActionTypes,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  checkDeployStatusAction,
  checkDeployStatusActionTypes,
  toggleWizardActionTypes
} from './types';

export const showDeployWizardForModel = (modelID: string) => ({ type: toggleWizardActionTypes.OPEN_WIZARD, payload: modelID });

export const closeDeployWizardForModel = (modelID: string) => ({ type: toggleWizardActionTypes.CLOSE_WIZARD });

export const loadDeployStatusForModels = (modelIds: string[]): ActionResult<void, any> => async (dispatch, getState) => {
  modelIds
    .filter(modelId => selectDeployStatusInfo(getState(), modelId).status === 'unknown')
    .forEach(modelId => loadDeployStatus(modelId)(dispatch, getState, undefined));
};

// todo rename
// todo handle error
export const deployWithCheckingStatus = (modelId: string): ActionResult<void, deployAction> => async (dispatch, getState) => {
  await deploy(modelId)(dispatch, getState, undefined);
  await checkDeployStatus(modelId)(dispatch, getState, undefined);
};

export const deploy = (modelId: string): ActionResult<void, deployAction> => async dispatch => {
  dispatch(action(deployActionTypes.DEPLOY_REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .deploy({} as any)
    .then(res => {
      dispatch(action(deployActionTypes.DEPLOY_SUCCESS, modelId));
    })
    .catch(err => {
      dispatch(action(deployActionTypes.DEPLOY_FAILURE, { modelId, error: err }));
    });
};

export const checkDeployStatus = (modelId: string): ActionResult<void, checkDeployStatusAction> => async (dispatch, getState) => {
  const isCheckingStatusInfo = selectIsLoadingDeployStatusInfo(getState(), modelId);
  // because now is running checkinging deploy status
  if (isCheckingStatusInfo) {
    return;
  }

  const modelDeployStatusInfo = selectDeployStatusInfo(getState(), modelId);
  if (modelDeployStatusInfo.status === 'notDeployed') {
    return;
  }
  if (modelDeployStatusInfo.status === 'deployed') {
    return;
  }
  setTimeout(async () => {
    await loadDeployStatus(modelId)(dispatch, getState, undefined);
    checkDeployStatus(modelId)(dispatch, getState, undefined);
  }, 1000);
};

export const loadDeployStatus = (modelId: string): ActionResult<void, loadDeployStatusAction> => async (
  dispatch
): Promise<IDeployStatusInfo> => {
  dispatch(action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_REQUEST, modelId));

  return await ServiceFactory.getDeployService()
    .loadStatus(modelId)
    .then(res => {
      dispatch(action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS, { modelId, info: res.data }));
      return res.data;
    })
    .catch(err => {
      dispatch(action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_FAILURE, { modelId, error: 'error' }));
      return err.data;
    });
};
