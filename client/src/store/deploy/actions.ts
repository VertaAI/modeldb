import { action } from 'typesafe-actions';

import { IDeployStatusInfo } from 'models/Deploy';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import {
  needCheckDeployStatus,
  selectDeployStatusInfo,
  selectIsLoadingDeployStatusInfo,
} from './selectors';
import {
  checkDeployStatusAction,
  deployAction,
  deployActionTypes,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  toggleWizardActionTypes,
} from './types';

export const showDeployWizardForModel = (modelID: string) => ({
  payload: modelID,
  type: toggleWizardActionTypes.OPEN_WIZARD,
});
export const closeDeployWizardForModel = (modelID: string) => ({
  type: toggleWizardActionTypes.CLOSE_WIZARD,
});

export const loadDeployStatusForModelsIfNeed = (
  modelIds: string[]
): ActionResult<void, any> => async (dispatch, getState) => {
  modelIds
    .filter(modelId => needCheckDeployStatus(getState(), modelId))
    .forEach(modelId =>
      checkDeployStatusUntilDeployed(modelId)(dispatch, getState, undefined)
    );
};

// todo rename
export const deployWithCheckingStatusUntilDeployed = (
  modelId: string
): ActionResult<void, deployAction> => async (dispatch, getState) => {
  await deploy(modelId)(dispatch, getState, undefined);
  await checkDeployStatusUntilDeployed(modelId)(dispatch, getState, undefined);
};

const deploy = (
  modelId: string
): ActionResult<void, deployAction> => async dispatch => {
  dispatch(action(deployActionTypes.DEPLOY_REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .deploy({ modelId } as any)
    .then(res => {
      dispatch(action(deployActionTypes.DEPLOY_SUCCESS, modelId));
    })
    .catch(err => {
      dispatch(
        action(deployActionTypes.DEPLOY_FAILURE, { modelId, error: err })
      );
    });
};

export const checkDeployStatusUntilDeployed = (
  modelId: string
): ActionResult<void, checkDeployStatusAction> => async (
  dispatch,
  getState
) => {
  const isCheckingStatusInfo = selectIsLoadingDeployStatusInfo(
    getState(),
    modelId
  );
  // because now is running checkinging deploy status
  if (isCheckingStatusInfo) {
    return;
  }

  const modelDeployStatusInfo = selectDeployStatusInfo(getState(), modelId);
  if (
    modelDeployStatusInfo.status === 'deployed' ||
    modelDeployStatusInfo.status === 'notDeployed'
  ) {
    return;
  }
  setTimeout(async () => {
    await loadDeployStatus(modelId)(dispatch, getState, undefined);
    checkDeployStatusUntilDeployed(modelId)(dispatch, getState, undefined);
  }, 1000);
};

export const loadDeployStatus = (
  modelId: string
): ActionResult<void, loadDeployStatusAction> => async (
  dispatch
): Promise<IDeployStatusInfo> => {
  dispatch(
    action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_REQUEST, modelId)
  );

  return await ServiceFactory.getDeployService()
    .loadStatus(modelId)
    .then(res => {
      dispatch(
        action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS, {
          modelId,
          info: res.data,
        })
      );
      return res.data;
    })
    .catch(err => {
      dispatch(
        action(loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_FAILURE, {
          modelId,
          error: 'error',
        })
      );
      return err.data;
    });
};
