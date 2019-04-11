import { action } from 'typesafe-actions';

import ModelRecord from 'models/ModelRecord';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import { fetchModelRecordAction, fetchModelRecordActionTypes } from './types';
import { fetchExperimentRuns } from 'store/experiment-runs/actions';

export const fetchModelRecord = (
  modelId: string
): ActionResult<void, fetchModelRecordAction> => async (dispatch, getState) => {
  if (getState().experimentRuns.data == null) {
    dispatch(fetchExperimentRuns(getState().location.projectId || ''));
  }
  dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST));

  // Need to fetch the experiment runs first
  var promise;
  if (!getState().experimentRuns.data) {
    promise = ServiceFactory.getExperimentRunsService()
      .getExperimentRuns(getState().location.projectId || '')
      .then(records => {
        const data = records.data;
        for (var i = 0; i < data.length; i++) {
          const record = data[i];
          if (record.id == modelId) return [record];
        }
        return [new ModelRecord()];
      });
  } else {
    promise = Promise.resolve(getState().experimentRuns.data);
  }

  await promise
    .then(storeExperimentRuns => {
      return ServiceFactory.getExperimentRunsService()
        .getModelRecord(modelId, storeExperimentRuns)
        .then(res => {
          dispatch(
            action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCCESS, res)
          );
        });
    })
    .catch(err => {
      dispatch(action(fetchModelRecordActionTypes.FETCH_MODEL_RECORD_FAILURE));
    });
};
