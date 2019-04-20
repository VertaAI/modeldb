import { action } from 'typesafe-actions';

import { IDeployStatusInfo } from 'models/Deploy';
import { ActionResult } from 'store/store';

import {
  needCheckDeployStatus,
  selectDeployStatusInfo,
  selectIsLoadingDeployStatusInfo,
} from './selectors';
import {
  deleteActionTypes,
  deployActionTypes,
  ICheckDeployStatusActions,
  IDeleteActions,
  IDeployActions,
  ILoadDataStatisticsActions,
  ILoadDeployStatusActions,
  ILoadServiceStatisticsActions,
  loadDataStatisticsActionTypes,
  loadDeployStatusActionTypes,
  loadServiceStatisticsActionTypes,
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
): ActionResult<void, IDeployActions> => async (dispatch, getState, deps) => {
  await deploy(modelId)(dispatch, getState, deps);
  await checkDeployStatusUntilDeployed(modelId)(dispatch, getState, deps);
};

const deploy = (modelId: string): ActionResult<void, IDeployActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(deployActionTypes.REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .deploy({ modelId } as any)
    .then(res => {
      dispatch(action(deployActionTypes.SUCCESS, modelId));
    })
    .catch(err => {
      dispatch(action(deployActionTypes.FAILURE, { modelId, error: err }));
    });
};

export const delete_ = (
  modelId: string
): ActionResult<void, IDeleteActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(deleteActionTypes.REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .delete(modelId)
    .then(() => {
      dispatch(action(deleteActionTypes.SUCCESS, modelId));
      dispatch(loadDeployStatus(modelId));
    })
    .catch(err => {
      dispatch(action(deleteActionTypes.FAILURE, { modelId, error: err }));
    });
};

export const checkDeployStatusUntilDeployed = (
  modelId: string
): ActionResult<void, ICheckDeployStatusActions> => async (
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
): ActionResult<void, ILoadDeployStatusActions> => async (
  dispatch,
  _,
  { ServiceFactory }
): Promise<IDeployStatusInfo> => {
  dispatch(action(loadDeployStatusActionTypes.REQUEST, modelId));

  return await ServiceFactory.getDeployService()
    .loadStatus(modelId)
    .then(res => {
      dispatch(
        action(loadDeployStatusActionTypes.SUCCESS, {
          modelId,
          info: res.data,
        })
      );
      return res.data;
    })
    .catch(err => {
      dispatch(
        action(loadDeployStatusActionTypes.FAILURE, {
          modelId,
          error: 'error',
        })
      );
      return err.data;
    });
};

export const getServiceStatistics = (
  modelId: string
): ActionResult<void, ILoadServiceStatisticsActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadServiceStatisticsActionTypes.REQUEST));

  await ServiceFactory.getDeployService()
    .getServiceStatistics(modelId)
    .then(res => {
      dispatch(action(loadServiceStatisticsActionTypes.SUCCESS, res.data));
    })
    .catch(err => {
      dispatch(action(loadServiceStatisticsActionTypes.FAILURE, err as string));
    });
};

export const getDataStatistics = (
  modelId: string
): ActionResult<void, ILoadDataStatisticsActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadDataStatisticsActionTypes.REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .getDataStatistics(modelId)
    .then(res => {
      dispatch(action(loadDataStatisticsActionTypes.SUCCESS, res.data));
    })
    .catch(err => {
      dispatch(action(loadDataStatisticsActionTypes.FAILURE, err as string));
    });
};
