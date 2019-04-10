import { action } from 'typesafe-actions';

import { IFilterData } from 'models/Filters';
import { ActionResult } from 'store/store';

import {
  ILoadExperimentRunsActions,
  loadExperimentRunsActionTypes,
} from './types';

export const fetchExperimentRuns = (
  id: string,
  filters?: IFilterData[]
): ActionResult<void, ILoadExperimentRunsActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadExperimentRunsActionTypes.REQUEST));

  await ServiceFactory.getExperimentRunsService()
    .getExperimentRuns(id, filters)
    .then(res => {
      dispatch(action(loadExperimentRunsActionTypes.SUCCESS, res.data));
    })
    .catch(err => {
      dispatch(action(loadExperimentRunsActionTypes.FAILURE, err as string));
    });
};
