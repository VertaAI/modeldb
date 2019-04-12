import { action } from 'typesafe-actions';

import ModelRecord from 'models/ModelRecord';
import { selectExperimentRuns } from 'store/experiment-runs';
import { fetchExperimentRuns } from 'store/experiment-runs/actions';
import { ActionResult } from 'store/store';

import { ILoadModelRecordActions, loadModelRecordActionTypes } from './types';

export const fetchModelRecord = (
  modelId: string
): ActionResult<void, ILoadModelRecordActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  const experimentRuns = selectExperimentRuns(getState());
  if (!experimentRuns) {
    dispatch(fetchExperimentRuns(getState().location.projectId || ''));
  }
  dispatch(action(loadModelRecordActionTypes.REQUEST));

  // Need to fetch the experiment runs first
  let promise;
  if (!experimentRuns) {
    promise = ServiceFactory.getExperimentRunsService()
      .getExperimentRuns(getState().location.projectId || '')
      .then(records => {
        const data = records.data;
        for (let i = 0; i < data.length; i++) {
          const record = data[i];
          if (record.id == modelId) return [record];
        }
        return [new ModelRecord()];
      });
  } else {
    promise = Promise.resolve(experimentRuns);
  }

  await promise
    .then(storeExperimentRuns => {
      return ServiceFactory.getExperimentRunsService()
        .getModelRecord(modelId, storeExperimentRuns)
        .then(res => {
          dispatch(action(loadModelRecordActionTypes.SUCCESS, res));
        });
    })
    .catch(err => {
      dispatch(action(loadModelRecordActionTypes.FAILURE, err as string));
    });
};
