import { action } from 'typesafe-actions';

import { IDeployStatusInfo } from 'models/Deploy';
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
  fetchDataStatisticsAction,
  fetchDataStatisticsActionTypes,
  fetchServiceStatisticsAction,
  fetchServiceStatisticsActionTypes,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  toggleDeployManagerActionTypes,
} from './types';

export const showDeployManagerForModel = (modelID: string) => ({
  payload: modelID,
  type: toggleDeployManagerActionTypes.OPEN_DEPLOY_MANAGER,
});
export const closeDeployManagerForModel = (modelID: string) => ({
  type: toggleDeployManagerActionTypes.CLOSE_DEPLOY_MANAGER,
});

export const checkDeployStatusForModelsIfNeed = (
  modelIds: string[]
): ActionResult<void, any> => async (dispatch, getState, deps) => {
  modelIds
    .filter(modelId => needCheckDeployStatus(getState(), modelId))
    .forEach(modelId =>
      checkDeployStatusUntilDeployed(modelId)(dispatch, getState, deps)
    );
};

// todo rename
export const deployWithCheckingStatusUntilDeployed = (
  modelId: string
): ActionResult<void, deployAction> => async (dispatch, getState, deps) => {
  await deploy(modelId)(dispatch, getState, deps);
  await checkDeployStatusUntilDeployed(modelId)(dispatch, getState, deps);
};

const deploy = (modelId: string): ActionResult<void, deployAction> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
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
  getState,
  deps
) => {
  const isCheckingStatusInfo = selectIsLoadingDeployStatusInfo(
    getState(),
    modelId
  );
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
    await loadDeployStatus(modelId)(dispatch, getState, deps);
    checkDeployStatusUntilDeployed(modelId)(dispatch, getState, deps);
  }, 1000);
};

export const loadDeployStatus = (
  modelId: string
): ActionResult<void, loadDeployStatusAction> => async (
  dispatch,
  _,
  { ServiceFactory }
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

export const getServiceStatistics = (
  modelId: string
): ActionResult<void, fetchServiceStatisticsAction> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(
    action(fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_REQUEST)
  );

  await ServiceFactory.getDeployService()
    .getServiceStatistics(modelId)
    .then(res => {
      dispatch(
        action(
          fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_SUCCESS,
          res.data
        )
      );
    })
    .catch(err => {
      dispatch(
        action(
          fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_FAILURE,
          err as string
        )
      );
    });
};

export const getDataStatistics = (
  modelId: string
): ActionResult<void, fetchDataStatisticsAction> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(
    action(
      fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_REQUEST,
      modelId
    )
  );

  await ServiceFactory.getDeployService()
    .getDataStatistics(modelId)
    .then(res => {
      dispatch(
        action(
          fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_SUCCESS,
          res.data
        )
      );
    })
    .catch(err => {
      dispatch(
        action(
          fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_FAILURE,
          err as string
        )
      );
    });
};
