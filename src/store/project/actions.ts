import Project from 'models/Project';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { ProjectActionTypes, projectFetchModelsAction, projectFetchModelsActionTypes } from './types';

export const fetchProjects = () => action(ProjectActionTypes.FETCH_PROJECTS);
export const fetchSuccess = (data: Project[]) => action(ProjectActionTypes.FETCH_SUCCESS, data);
export const addProject = (data: Project) => action(ProjectActionTypes.ADD_PROJECT, data);
export const removeProjects = (ids: number[]) => action(ProjectActionTypes.REMOVE_PROJECTS, ids);

export const fetchProjectWithModels = (id: string): ActionResult<void, projectFetchModelsAction> => async (dispatch, getState) => {
  dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_REQUEST));

  await new Promise<Project>((resolve, reject) => {
    resolve(ServiceFactory.getDataService().getProject(id));
  }).then(res => {
    dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_SUCESS, res));
  });
};
