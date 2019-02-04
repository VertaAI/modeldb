import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ModelRecord from '../../models/ModelRecord';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchModelRecordAction, fetchModelRecordActionTypes } from './types';

export const fetchModelRecord = (modelId: string): ActionResult<void, fetchModelRecordAction> => async (dispatch, getState) => {
  dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST));

  const storeExperimentRuns = getState().experimentRuns.data || [new ModelRecord()];
  await ServiceFactory.getExperimentRunsService()
    .getModelRecord(modelId, storeExperimentRuns)
    .then(res => {
      dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCESS, res));
    });
};
