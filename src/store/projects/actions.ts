import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import Project, { UserAccess } from '../../models/Project';
import User from '../../models/User';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchProjectsAction, fetchProjectsActionTypes, IUpdateProjectAction, updateProjectActionTypes } from './types';

export const fetchProjects = (): ActionResult<void, fetchProjectsAction> => async (dispatch, getState) => {
  dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));

  await ServiceFactory.getProjectsService()
    .getProjects()
    .then(res => {
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_SUCCESS, res));
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
  const { projects } = getState();
  const project = projects.data!.find((value: Project, index: number) => value.Id === projectId)!;

  if (userAccess === UserAccess.Owner) {
    project.Author = user;

    const collaborators = Array.from(project.Collaborators).slice(1);
    project.Collaborators.clear();
    project.Collaborators.set(project.Author, UserAccess.Owner);

    for (const collaborator of collaborators) {
      const [collaboratorUser, collaboratorUserAccess] = collaborator;
      project.Collaborators.set(collaboratorUser, collaboratorUserAccess);
    }
  } else {
    const projectIndex = projects.data!.indexOf(project);
    project.Collaborators.set(user, userAccess);

    projects.data![projectIndex] = project;
  }
  dispatch(action(updateProjectActionTypes.UPDATE_PROJECT_STATE, projects.data!));
};

export const removeCollaboratorFromProject = (projectId: string, user: User): ActionResult<void, IUpdateProjectAction> => async (
  dispatch,
  getState
) => {
  const { projects } = getState();
  const project = projects.data!.find((value: Project, index: number) => value.Id === projectId)!;
  project.Collaborators.delete(user);

  const projectIndex = projects.data!.indexOf(project);
  projects.data![projectIndex] = project;

  dispatch(action(updateProjectActionTypes.UPDATE_PROJECT_STATE, projects.data!));
};
