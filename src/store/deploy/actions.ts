import { action } from 'typesafe-actions';

import { ActionResult } from 'store/store';
import ServiceFactory from 'services/ServiceFactory';
import { UserAccess } from 'models/Project';
import { IDeployConfig } from 'models/Deploy';

import { deployAction, deployActionTypes } from './types';

export const deploy = (modelId: string, config: IDeployConfig): ActionResult<void, deployAction> => async (dispatch, getState) => {
  dispatch(action(deployActionTypes.DEPLOY_REQUEST, modelId));

  await ServiceFactory.getDeployService()
    .deploy(modelId, config)
    .then(res => {
      dispatch(action(deployActionTypes.DEPLOY_SUCCESS, res));
    })
    .catch(err => {
      dispatch(action(deployActionTypes.DEPLOY_FAILURE, { modelId, error: 'error' }));
    });
};
