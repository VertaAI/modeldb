import { UserAccess } from 'models/Project';
import { AnyAction } from 'redux';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { IResetInvitationAction, resetInvitationActionTypes, sendInvitationAction, sendInvitationActionTypes } from './types';

export const sendInvitationForUser = (email: string, userAccess: UserAccess): ActionResult<void, sendInvitationAction> => async (
  dispatch,
  getState
) => {
  dispatch(action(sendInvitationActionTypes.SEND_INVITATION_REQUEST));

  await ServiceFactory.getCollaboratorsService()
    .sendInvitation(email, userAccess)
    .then(res => {
      dispatch(action(sendInvitationActionTypes.SEND_INVITATION_SUCCESS, true));
    })
    .catch(err => {
      dispatch(action(sendInvitationActionTypes.SEND_INVITATION_FAILURE));
    });
};

export const resetInvitationState = (): ActionResult<void, IResetInvitationAction> => async (dispatch, getState) => {
  dispatch(action(resetInvitationActionTypes.RESET_INVITATION_STATE));
};
