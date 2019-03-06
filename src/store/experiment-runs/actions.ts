import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { IFilterData } from '../../models/Filters';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchExperimentRunsAction, fetchExperimentRunsActionTypes } from './types';

export const fetchExperimentRuns = (id: string, filters?: IFilterData[]): ActionResult<void, fetchExperimentRunsAction> => async (
  dispatch,
  getState
) => {
  dispatch(action(fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_REQUEST));

  await ServiceFactory.getExperimentRunsService()
    .getExperimentRuns(id, filters)
    .then(res => {
      dispatch(action(fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_SUCCESS, res.data));
    })
    .catch(err => {
      dispatch(action(fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_FAILURE));
    });
};
