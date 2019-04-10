import { combineReducers } from 'redux';

import {
  makeCommunicationReducerFromEnum,
  makeResetCommunicationReducer,
} from 'utils/redux/communication';
import composeReducers from 'utils/redux/composeReducers';

import {
  changeAccessActionTypes,
  changeOwnerActionTypes,
  ICollaborationState,
  removeAccessActionTypes,
  resetChangeAccessActionTypes,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
  resetRemoveAccessActionTypes,
  sendInvitationActionTypes,
} from '../types';

export default combineReducers<ICollaborationState['communications']>({
  sendingInvitation: composeReducers([
    makeCommunicationReducerFromEnum(sendInvitationActionTypes),
    makeResetCommunicationReducer(
      resetInvitationActionTypes.RESET_INVITATION_STATE
    ),
  ]),
  changingOwner: composeReducers([
    makeCommunicationReducerFromEnum(changeOwnerActionTypes),
    makeResetCommunicationReducer(
      resetChangeOwnerActionTypes.RESET_CHANGE_OWNER
    ),
  ]),
  changingAccess: composeReducers([
    makeCommunicationReducerFromEnum(changeAccessActionTypes),
    makeResetCommunicationReducer(
      resetChangeAccessActionTypes.RESET_CHANGE_ACCESS
    ),
  ]),
  removingAccess: composeReducers([
    makeCommunicationReducerFromEnum(removeAccessActionTypes),
    makeResetCommunicationReducer(
      resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS
    ),
  ]),
});
