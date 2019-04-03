import { action } from 'typesafe-actions';

import { IFilterData } from 'models/Filters';
import { Project, UserAccess } from 'models/Project';
import User from 'models/User';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import { selectProjects } from './selectors';
import {
  fetchProjectsAction,
  fetchProjectsActionTypes,
  IUpdateProjectAction,
  updateProjectActionTypes,
} from './types';

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
    .catch(err => {
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));
    });
};

export const updateProjectCollaboratorAccess = (
  projectId: string,
  user: User,
  userAccess: UserAccess
): ActionResult<void, IUpdateProjectAction> => async (dispatch, getState) => {
  const projects = selectProjects(getState())!;
  const project = projects.find(value => value.id === projectId)!;

  if (userAccess === UserAccess.Owner) {
    project.Author = user;

    const collaborators = Array.from(project.collaborators).slice(1);
    project.collaborators.clear();
    project.collaborators.set(project.Author, UserAccess.Owner);

    for (const collaborator of collaborators) {
      const [collaboratorUser, collaboratorUserAccess] = collaborator;
      project.collaborators.set(collaboratorUser, collaboratorUserAccess);
    }
  } else {
    const projectIndex = projects.indexOf(project);
    project.collaborators.set(user, userAccess);

    projects[projectIndex] = project;
  }
  dispatch(action(updateProjectActionTypes.UPDATE_PROJECT_STATE, projects));
};

export const removeCollaboratorFromProject = (
  projectId: string,
  user: User
): ActionResult<void, IUpdateProjectAction> => async (dispatch, getState) => {
  const projects = selectProjects(getState())!;
  const project = projects.find(value => value.id === projectId)!;
  project.collaborators.delete(user);

  const projectIndex = projects.indexOf(project);
  projects[projectIndex] = project;

  dispatch(action(updateProjectActionTypes.UPDATE_PROJECT_STATE, projects));
};
