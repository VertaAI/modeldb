import Project from 'models/Project';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchProjectsAction, fetchProjectsActionTypes, projectFetchModelsAction, projectFetchModelsActionTypes } from './types';

export const fetchProjects = (): ActionResult<void, fetchProjectsAction> => async (dispatch, getState) => {
  dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));

  await ServiceFactory.getDataService()
    .getProjects()
    .then(res => {
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_SUCESS, res));
    });
};

export const fetchProjectWithModels = (id: string): ActionResult<void, projectFetchModelsAction> => async (dispatch, getState) => {
  dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_REQUEST));

  await ServiceFactory.getDataService()
    .getProject(id)
    .then(res => {
      dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_SUCESS, res));
    });
};
