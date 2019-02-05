import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchExperimentRunsAction, fetchExperimentRunsActionTypes } from './types';

export const fetchExperimentRuns = (id: string): ActionResult<void, fetchExperimentRunsAction> => async (dispatch, getState) => {
  dispatch(action(fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_REQUEST));

  await ServiceFactory.getExperimentRunsService()
    .getExperimentRuns(id)
    .then(res => {
      dispatch(action(fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_SUCCESS, res));
    });
};
