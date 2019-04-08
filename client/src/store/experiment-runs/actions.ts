import { action } from 'typesafe-actions';

import { IFilterData } from 'models/Filters';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import {
  ILoadExperimentRunsActions,
  loadExperimentRunsActionTypes,
} from './types';

export const fetchExperimentRuns = (
  id: string,
  filters?: IFilterData[]
): ActionResult<void, ILoadExperimentRunsActions> => async dispatch => {
  dispatch(action(loadExperimentRunsActionTypes.request));

  await ServiceFactory.getExperimentRunsService()
    .getExperimentRuns(id, filters)
    .then(res => {
      dispatch(action(loadExperimentRunsActionTypes.success, res.data));
    })
    .catch(err => {
      dispatch(action(loadExperimentRunsActionTypes.failure, err as string));
    });
};
