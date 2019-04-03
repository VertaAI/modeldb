import { action } from 'typesafe-actions';

import ModelRecord from 'models/ModelRecord';
import ServiceFactory from 'services/ServiceFactory';
import { selectExperimentRuns } from 'store/experiment-runs';
import { ActionResult } from 'store/store';

import { fetchModelRecordAction, fetchModelRecordActionTypes } from './types';

export const fetchModelRecord = (
  modelId: string
): ActionResult<void, fetchModelRecordAction> => async (dispatch, getState) => {
  dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST));

  const storeExperimentRuns = selectExperimentRuns(getState()) || [
    new ModelRecord(),
  ];
  await ServiceFactory.getExperimentRunsService()
    .getModelRecord(modelId, storeExperimentRuns)
    .then(res => {
      dispatch(
        action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCCESS, res)
      );
    })
    .catch(err => {
      dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_FAILURE));
    });
};
