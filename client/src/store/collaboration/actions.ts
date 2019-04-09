import { UserAccess } from 'models/Project';
import User from 'models/User';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import {
  removeCollaboratorFromProject,
  updateProjectCollaboratorAccess,
} from '../projects';
import {
  changeAccessActionTypes,
  changeOwnerActionTypes,
  IChangeAccessActions,
  IChangeOwnerActions,
  IRemoveAccessActions,
  IResetChangeAccessAction,
  IResetChangeOwnerAction,
  IResetInvitationAction,
  IResetRemoveAccessAction,
  ISendInvitationActions,
  removeAccessActionTypes,
  resetChangeAccessActionTypes,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
  resetRemoveAccessActionTypes,
  sendInvitationActionTypes,
} from './types';

export const sendInvitationForUser = (
  projectId: string,
  email: string,
  userAccess: UserAccess
): ActionResult<void, ISendInvitationActions> => async dispatch => {
  dispatch(action(sendInvitationActionTypes.request));

  await ServiceFactory.getCollaboratorsService()
    .sendInvitation(projectId, email, userAccess)
    .then(res => {
      dispatch(action(sendInvitationActionTypes.success));
      dispatch(
        updateProjectCollaboratorAccess(
          projectId,
          new User(undefined, email),
          userAccess
        )
      );
    })
    .catch(err => {
      dispatch(action(sendInvitationActionTypes.failure, err as string));
    });
};

export const resetInvitationState = (): ActionResult<
  void,
  IResetInvitationAction
> => async dispatch => {
  dispatch(action(resetInvitationActionTypes.RESET_INVITATION_STATE));
};

export const changeProjectOwner = (
  projectId: string,
  email: string
): ActionResult<void, IChangeOwnerActions> => async dispatch => {
  dispatch(action(changeOwnerActionTypes.request));

  await ServiceFactory.getCollaboratorsService()
    .changeOwner(projectId, email)
    .then(res => {
      dispatch(action(changeOwnerActionTypes.success));
      dispatch(
        updateProjectCollaboratorAccess(
          projectId,
          new User(undefined, email),
          UserAccess.Owner
        )
      );
    })
    .catch(err => {
      dispatch(action(changeOwnerActionTypes.failure, err as string));
    });
};

export const resetChangeOwnerState = (): ActionResult<
  void,
  IResetChangeOwnerAction
> => async dispatch => {
  dispatch(action(resetChangeOwnerActionTypes.RESET_CHANGE_OWNER));
};

export const changeAccessToProject = (
  projectId: string,
  user: User,
  userAccess: UserAccess
): ActionResult<void, IChangeAccessActions> => async dispatch => {
  dispatch(action(changeAccessActionTypes.request));

  await ServiceFactory.getCollaboratorsService()
    .changeAccessToProject(projectId, user.email, userAccess)
    .then(res => {
      dispatch(action(changeAccessActionTypes.success));
      dispatch(updateProjectCollaboratorAccess(projectId, user, userAccess));
    })
    .catch(err => {
      dispatch(action(changeAccessActionTypes.failure, err as string));
    });
};

export const resetChangeAccessState = (): ActionResult<
  void,
  IResetChangeAccessAction
> => async dispatch => {
  dispatch(action(resetChangeAccessActionTypes.RESET_CHANGE_ACCESS));
};

export const removeAccessFromProject = (
  projectId: string,
  user: User
): ActionResult<void, IRemoveAccessActions> => async dispatch => {
  dispatch(action(removeAccessActionTypes.request));

  await ServiceFactory.getCollaboratorsService()
    .removeAccessFromProject(projectId, user.email)
    .then(res => {
      dispatch(action(removeAccessActionTypes.success));
      dispatch(removeCollaboratorFromProject(projectId, user));
    })
    .catch(err => {
      dispatch(action(removeAccessActionTypes.failure, err as string));
    });
};

export const resetRemoveAccessState = (): ActionResult<
  void,
  IResetRemoveAccessAction
> => async dispatch => {
  dispatch(action(resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS));
};
