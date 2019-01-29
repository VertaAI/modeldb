import Project from 'models/Project';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import {
  apiProjectsAction,
  fetchProjectsAction,
  fetchProjectsActionTypes,
  projectFetchModelsAction,
  projectFetchModelsActionTypes
} from './types';

export const fetchProjects = (): ActionResult<void, fetchProjectsAction> => async (dispatch, getState) => {
  dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));

  await ServiceFactory.getDataService()
    .getProjects()
    .then(res => {
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_SUCESS, res));
    });
};

export const apiFetchProjects = (): ActionResult<void, apiProjectsAction> => async (dispatch, getState) => {
  dispatch(action(fetchProjectsActionTypes.API_PROJECTS_REQUEST));

  fetch('http://localhost:8080/v1/example/getProjects', { method: 'post' })
    .then(res => {
      if (!res.ok) {
        throw Error(res.statusText);
      }
      return res.json();
    })
    .then(res => {
      console.log(res.projects);
      dispatch({ type: fetchProjectsActionTypes.API_PROJECTS_SUCESS, payload: res.projects });
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

// Error at dispatch
// { type: fetchProjectsActionTypes.API_PROJECTS_SUCESS; } |
// { type: fetchProjectsActionTypes.API_PROJECTS_SUCESS; payload: any; }'
// is not assignable to parameter of type 'ThunkAction<{}, IApplicationState, undefined, fetchProjectsAction>'.

// Type '{ type: fetchProjectsActionTypes.API_PROJECTS_SUCESS; }'
// is not assignable to type 'ThunkAction<{}, IApplicationState, undefined, fetchProjectsAction>'.

// Type '{ type: fetchProjectsActionTypes.API_PROJECTS_SUCESS; }'
// provides no match for the signature
// '(dispatch: ThunkDispatch<IApplicationState, undefined, fetchProjectsAction>,
// getState: () => IApplicationState, extraArgument: undefined): {}'.
