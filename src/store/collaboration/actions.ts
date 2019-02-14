import { AnyAction } from 'redux';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import Project, { UserAccess } from '../../models/Project';
import User from '../../models/User';
import ServiceFactory from '../../services/ServiceFactory';
import { updateProjectsCollaborator } from '../projects';
import {
  changeOwnerAction,
  changeOwnerActionTypes,
  IResetChangeOwnerAction,
  IResetInvitationAction,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
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
      dispatch(action(sendInvitationActionTypes.SEND_INVITATION_SUCCESS, true));
      dispatch(updateProjectsCollaborator(projectId, email, userAccess));
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
      dispatch(action(changeOwnerActionTypes.CHANGE_OWNER_SUCCESS, true));
      dispatch(updateProjectsCollaborator(projectId, email, UserAccess.Owner));
    })
    .catch(err => {
      dispatch(action(changeOwnerActionTypes.CHANGE_OWNER_FAILURE));
    });
};

export const resetChangeOwnerState = (): ActionResult<void, IResetChangeOwnerAction> => async (dispatch, getState) => {
  dispatch(action(resetChangeOwnerActionTypes.RESET_CHANGE_OWNER));
};
