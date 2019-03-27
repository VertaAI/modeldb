import { action } from 'typesafe-actions';

import { IFilterData } from 'models/Filters';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import { fetchProjectsAction, fetchProjectsActionTypes } from './types';

export const fetchProjects = (
  filters?: IFilterData[]
): ActionResult<void, fetchProjectsAction> => async dispatch => {
  dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));

  await ServiceFactory.getProjectsService()
    .getProjects(filters)
    .then(res => {
      dispatch(
        action(fetchProjectsActionTypes.FETCH_PROJECTS_SUCCESS, res.data)
      );
    })
    .catch(() => {
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));
    });
};
