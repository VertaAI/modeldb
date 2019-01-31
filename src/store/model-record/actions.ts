import ModelRecord from '../../models/ModelRecord';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchModelRecordAction, fetchModelRecordActionTypes } from './types';

export const fetchModelRecord = (model_id: string): ActionResult<void, fetchModelRecordAction> => async (dispatch, getState) => {
  dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST));

  // if (getState().experiment_runs.data) {

  // }

  let store_experiment_runs = getState().experiment_runs.data || [new ModelRecord()];
  await ServiceFactory.getExperimentRunsService()
    .getModelRecord(model_id, store_experiment_runs)
    .then(res => {
      dispatch({ type: fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCESS, payload: res });
    });
};
