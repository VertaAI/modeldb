import { action } from 'typesafe-actions';

import ModelRecord from 'models/ModelRecord';
import ServiceFactory from 'services/ServiceFactory';
import { selectExperimentRuns } from 'store/experiment-runs';
import { ActionResult } from 'store/store';

import { ILoadModelRecordActions, loadModelRecordActionTypes } from './types';

export const fetchModelRecord = (
  modelId: string
): ActionResult<void, ILoadModelRecordActions> => async (
  dispatch,
  getState
) => {
  dispatch(action(loadModelRecordActionTypes.REQUEST));

  const storeExperimentRuns = selectExperimentRuns(getState()) || [
    new ModelRecord(),
  ];
  await ServiceFactory.getExperimentRunsService()
    .getModelRecord(modelId, storeExperimentRuns)
    .then(res => {
      dispatch(action(loadModelRecordActionTypes.SUCCESS, res));
    })
    .catch(err => {
      dispatch(action(loadModelRecordActionTypes.FAILURE, err as string));
    });
};
