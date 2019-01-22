import { Model } from 'models/Model';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchModelAction, fetchModelActionTypes } from './types';

export const fetchModel = (id: string): ActionResult<void, fetchModelAction> => async (dispatch, getState) => {
  dispatch(action(fetchModelActionTypes.FETCH_MODEL_REQUEST));

  await new Promise<Model>((resolve, reject) => {
    resolve(ServiceFactory.getDataService().getModel(id));
  }).then(res => {
    dispatch(action(fetchModelActionTypes.FETCH_MODEL_SUCESS, res));
  });
};
