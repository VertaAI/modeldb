import { Reducer } from 'redux';
import {
  changeOwnerAction,
  changeOwnerActionTypes,
  ICollaborationState,
  IResetChangeOwnerAction,
  IResetInvitationAction,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
  sendInvitationAction,
  sendInvitationActionTypes
} from './types';

const collaborationInitialState: ICollaborationState = {
  changeOwner: {
    sending: false
  },
  inviteNewCollaborator: { sending: false }
};

const sendInvitationReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: sendInvitationAction) => {
  switch (action.type) {
    case sendInvitationActionTypes.SEND_INVITATION_REQUEST: {
      return { ...state, inviteNewCollaborator: { sending: true, result: undefined } };
    }
    case sendInvitationActionTypes.SEND_INVITATION_SUCCESS: {
      return { ...state, inviteNewCollaborator: { sending: false, result: true } };
    }
    case sendInvitationActionTypes.SEND_INVITATION_FAILURE: {
      return { ...state, inviteNewCollaborator: { sending: false, result: false } };
    }
    default: {
      return state;
    }
  }
};

const resetInvitationReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetInvitationAction) => {
  switch (action.type) {
    case resetInvitationActionTypes.RESET_INVITATION_STATE: {
      return { ...state, inviteNewCollaborator: { sending: false, result: undefined } };
    }
    default: {
      return state;
    }
  }
};

const changeOwnerReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: changeOwnerAction) => {
  switch (action.type) {
    case changeOwnerActionTypes.CHANGE_OWNER_REQUEST: {
      return { ...state, changeOwner: { sending: true, result: undefined } };
    }
    case changeOwnerActionTypes.CHANGE_OWNER_SUCCESS: {
      return { ...state, changeOwner: { sending: false, result: true } };
    }
    case changeOwnerActionTypes.CHANGE_OWNER_FAILURE: {
      return { ...state, changeOwner: { sending: false, result: false } };
    }
    default: {
      return state;
    }
  }
};

const resetChangeOwnerReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetChangeOwnerAction) => {
  switch (action.type) {
    case resetChangeOwnerActionTypes.RESET_CHANGE_OWNER: {
      return { ...state, changeOwner: { sending: false, result: undefined } };
    }
    default: {
      return state;
    }
  }
};

export const collaborationReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action) => {
  if (Object.values(sendInvitationActionTypes).includes(action.type)) {
    return sendInvitationReducer(state, action);
  }
  if (Object.values(resetInvitationActionTypes).includes(action.type)) {
    return resetInvitationReducer(state, action);
  }
  if (Object.values(changeOwnerActionTypes).includes(action.type)) {
    return changeOwnerReducer(state, action);
  }
  if (Object.values(resetInvitationActionTypes).includes(action.type)) {
    return resetInvitationReducer(state, action);
  }
  if (Object.values(resetChangeOwnerReducer).includes(action.type)) {
    return resetChangeOwnerReducer(state, action);
  }
  return state;
};
