import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { UserAccess } from '../../models/Project';
import User from '../../models/User';
import ServiceFactory from '../../services/ServiceFactory';
import { removeCollaboratorFromProject, updateProjectCollaboratorAccess } from '../projects';
import {
  changeAccessAction,
  changeAccessActionTypes,
  changeOwnerAction,
  changeOwnerActionTypes,
  InvitationStatus,
  IResetChangeAccessAction,
  IResetChangeOwnerAction,
  IResetInvitationAction,
  IResetRemoveAccessAction,
  removeAccessAction,
  removeAccessActionTypes,
  resetChangeAccessActionTypes,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
  resetRemoveAccessActionTypes,
  sendInvitationAction,
  sendInvitationActionTypes
} from './types';

export const sendInvitationForUser = (
  projectId: string,
  email: string,
  userAccess: UserAccess
): ActionResult<void, sendInvitationAction> => async (dispatch, getState) => {
  dispatch(action(sendInvitationActionTypes.SEND_INVITATION_REQUEST));

  await ServiceFactory.getCollaboratorsService()
    .sendInvitation(projectId, email, userAccess)
    .then(res => {
      dispatch(action(sendInvitationActionTypes.SEND_INVITATION_SUCCESS, InvitationStatus.Success));
      dispatch(updateProjectCollaboratorAccess(projectId, new User(email), userAccess));
    })
    .catch(err => {
      dispatch(action(sendInvitationActionTypes.SEND_INVITATION_FAILURE));
    });
};

export const resetInvitationState = (): ActionResult<void, IResetInvitationAction> => async (dispatch, getState) => {
  dispatch(action(resetInvitationActionTypes.RESET_INVITATION_STATE));
};

export const changeProjectOwner = (projectId: string, email: string): ActionResult<void, changeOwnerAction> => async (
  dispatch,
  getState
) => {
  dispatch(action(changeOwnerActionTypes.CHANGE_OWNER_REQUEST));

  await ServiceFactory.getCollaboratorsService()
    .changeOwner(projectId, email)
    .then(res => {
      dispatch(action(changeOwnerActionTypes.CHANGE_OWNER_SUCCESS, InvitationStatus.Success));
      dispatch(updateProjectCollaboratorAccess(projectId, new User(email), UserAccess.Owner));
    })
    .catch(err => {
      dispatch(action(changeOwnerActionTypes.CHANGE_OWNER_FAILURE));
    });
};

export const resetChangeOwnerState = (): ActionResult<void, IResetChangeOwnerAction> => async (dispatch, getState) => {
  dispatch(action(resetChangeOwnerActionTypes.RESET_CHANGE_OWNER));
};

export const changeAccessToProject = (
  projectId: string,
  user: User,
  userAccess: UserAccess
): ActionResult<void, changeAccessAction> => async (dispatch, getState) => {
  dispatch(action(changeAccessActionTypes.CHANGE_ACCESS_REQUEST));

  await ServiceFactory.getCollaboratorsService()
    .changeAccessToProject(projectId, user.email, userAccess)
    .then(res => {
      dispatch(action(changeAccessActionTypes.CHANGE_ACCESS_SUCCESS, InvitationStatus.Success));
      dispatch(updateProjectCollaboratorAccess(projectId, user, userAccess));
    })
    .catch(err => {
      dispatch(action(changeAccessActionTypes.CHANGE_ACCESS_FAILURE));
    });
};

export const resetChangeAccessState = (): ActionResult<void, IResetChangeAccessAction> => async (dispatch, getState) => {
  dispatch(action(resetChangeAccessActionTypes.RESET_CHANGE_ACCESS));
};

export const removeAccessFromProject = (projectId: string, user: User): ActionResult<void, removeAccessAction> => async (
  dispatch,
  getState
) => {
  dispatch(action(removeAccessActionTypes.REMOVE_ACCESS_REQUEST));

  await ServiceFactory.getCollaboratorsService()
    .removeAccessFromProject(projectId, user.email)
    .then(res => {
      dispatch(action(removeAccessActionTypes.REMOVE_ACCESS_SUCCESS, InvitationStatus.Success));
      dispatch(removeCollaboratorFromProject(projectId, user));
    })
    .catch(err => {
      dispatch(action(removeAccessActionTypes.REMOVE_ACCESS_FAILURE));
    });
};

export const resetRemoveAccessState = (): ActionResult<void, IResetRemoveAccessAction> => async (dispatch, getState) => {
  dispatch(action(resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS));
};
