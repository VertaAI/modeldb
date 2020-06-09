import { action } from 'typesafe-actions';

import normalizeError from 'shared/utils/normalizeError';
import { IProjectCreationSettings } from 'shared/models/Project';
import routes from 'shared/routes';
import { ActionResult } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import {
  createProjectActionTypes,
  ICreateProjectActionTypes,
  IResetCreateProjectCommunication,
  resetCreateProjectCommunicationActionTypes,
} from './types';

export const createProject = (
  settings: IProjectCreationSettings
): ActionResult<void, ICreateProjectActionTypes> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(createProjectActionTypes.REQUEST, {
      settings,
    })
  );

  return deps.ServiceFactory.getProjectsService()
    .createProject(settings)
    .then(project => {
      dispatch(action(createProjectActionTypes.SUCCESS, { project }));
      deps.history.push(
        routes.projectSummary.getRedirectPath({
          projectId: project.id,
          workspaceName: selectCurrentWorkspaceName(getState()),
        })
      );
    })
    .catch(error => {
      dispatch(action(createProjectActionTypes.FAILURE, normalizeError(error)));
    });
};

export const resetCreateProjectCommunication = (): IResetCreateProjectCommunication => ({
  type:
    resetCreateProjectCommunicationActionTypes.RESET_CREATE_PROJECT_COMMUNICATION,
});
