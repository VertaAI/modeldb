import { action } from 'typesafe-actions';

import { IFilterData } from 'models/Filters';
import { UserAccess, Project } from 'models/Project';
import User from 'models/User';
import { ActionResult } from 'store/store';
import cloneClassInstance from 'utils/cloneClassInstance';

import { selectProjects } from './selectors';
import {
  ILoadProjectsActions,
  IUpdateProjectAction,
  loadProjectsActionTypes,
  updateProjectActionTypes,
  IUpdateProjectByIdAction,
  updateProjectByIdActionTypes,
} from './types';

export const fetchProjects = (
  filters?: IFilterData[]
): ActionResult<void, ILoadProjectsActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadProjectsActionTypes.REQUEST));

  await ServiceFactory.getProjectsService()
    .getProjects(filters)
    .then(res => {
      dispatch(action(loadProjectsActionTypes.SUCCESS, res.data));
    })
    .catch(err => {
      dispatch(action(loadProjectsActionTypes.FAILURE, err as string));
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
    project.collaborators = new Map(project.collaborators);

    projects[projectIndex] = cloneClassInstance(project);
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
  project.collaborators = new Map(project.collaborators);

  const projectIndex = projects.indexOf(project);
  projects[projectIndex] = cloneClassInstance(project);

  dispatch(action(updateProjectActionTypes.UPDATE_PROJECT_STATE, projects));
};

export const updateProjectById = (
  project: Project
): IUpdateProjectByIdAction => ({
  type: updateProjectByIdActionTypes.UPDATE_PROJECT,
  payload: project,
});
