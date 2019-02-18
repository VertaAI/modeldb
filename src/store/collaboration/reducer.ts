import { Reducer } from 'redux';
import {
  changeAccessAction,
  changeAccessActionTypes,
  changeOwnerAction,
  changeOwnerActionTypes,
  ICollaborationState,
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

const collaborationInitialState: ICollaborationState = {
  changeAccess: { status: InvitationStatus.None },
  changeOwner: { status: InvitationStatus.None },
  inviteNewCollaborator: { status: InvitationStatus.None },
  removeAccess: { status: InvitationStatus.None }
};

const sendInvitationReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: sendInvitationAction) => {
  switch (action.type) {
    case sendInvitationActionTypes.SEND_INVITATION_REQUEST: {
      return { ...state, inviteNewCollaborator: { status: InvitationStatus.Sending } };
    }
    case sendInvitationActionTypes.SEND_INVITATION_SUCCESS: {
      return { ...state, inviteNewCollaborator: { status: InvitationStatus.Success } };
    }
    case sendInvitationActionTypes.SEND_INVITATION_FAILURE: {
      return { ...state, inviteNewCollaborator: { status: InvitationStatus.Failure } };
    }
    default: {
      return state;
    }
  }
};

const resetInvitationReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetInvitationAction) => {
  switch (action.type) {
    case resetInvitationActionTypes.RESET_INVITATION_STATE: {
      return { ...state, inviteNewCollaborator: { status: InvitationStatus.None } };
    }
    default: {
      return state;
    }
  }
};

const changeOwnerReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: changeOwnerAction) => {
  switch (action.type) {
    case changeOwnerActionTypes.CHANGE_OWNER_REQUEST: {
      return { ...state, changeOwner: { status: InvitationStatus.Sending } };
    }
    case changeOwnerActionTypes.CHANGE_OWNER_SUCCESS: {
      return { ...state, changeOwner: { status: InvitationStatus.Success } };
    }
    case changeOwnerActionTypes.CHANGE_OWNER_FAILURE: {
      return { ...state, changeOwner: { status: InvitationStatus.Failure } };
    }
    default: {
      return state;
    }
  }
};

const resetChangeOwnerReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetChangeOwnerAction) => {
  switch (action.type) {
    case resetChangeOwnerActionTypes.RESET_CHANGE_OWNER: {
      return { ...state, changeOwner: { status: InvitationStatus.None } };
    }
    default: {
      return state;
    }
  }
};

const changeAccessReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: changeAccessAction) => {
  switch (action.type) {
    case changeAccessActionTypes.CHANGE_ACCESS_REQUEST: {
      return { ...state, changeAccess: { status: InvitationStatus.Sending } };
    }
    case changeAccessActionTypes.CHANGE_ACCESS_SUCCESS: {
      return { ...state, changeAccess: { status: InvitationStatus.Success } };
    }
    case changeAccessActionTypes.CHANGE_ACCESS_FAILURE: {
      return { ...state, changeAccess: { status: InvitationStatus.Failure } };
    }
    default: {
      return state;
    }
  }
};

const resetChangeAccessReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetChangeAccessAction) => {
  switch (action.type) {
    case resetChangeAccessActionTypes.RESET_CHANGE_ACCESS: {
      return { ...state, changeAccess: { status: InvitationStatus.None } };
    }
    default: {
      return state;
    }
  }
};

const removeAccessReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: removeAccessAction) => {
  switch (action.type) {
    case removeAccessActionTypes.REMOVE_ACCESS_REQUEST: {
      return { ...state, changeAccess: { status: InvitationStatus.Sending } };
    }
    case removeAccessActionTypes.REMOVE_ACCESS_SUCCESS: {
      return { ...state, changeAccess: { status: InvitationStatus.Success } };
    }
    case removeAccessActionTypes.REMOVE_ACCESS_FAILURE: {
      return { ...state, changeAccess: { status: InvitationStatus.Failure } };
    }
    default: {
      return state;
    }
  }
};

const resetRemoveAccessReducer: Reducer<ICollaborationState> = (state = collaborationInitialState, action: IResetRemoveAccessAction) => {
  switch (action.type) {
    case resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS: {
      return { ...state, changeAccess: { status: InvitationStatus.None } };
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
  if (Object.values(resetChangeOwnerActionTypes).includes(action.type)) {
    return resetChangeOwnerReducer(state, action);
  }
  if (Object.values(changeAccessActionTypes).includes(action.type)) {
    return changeAccessReducer(state, action);
  }
  if (Object.values(resetChangeAccessActionTypes).includes(action.type)) {
    return resetChangeAccessReducer(state, action);
  }
  if (Object.values(removeAccessActionTypes).includes(action.type)) {
    return removeAccessReducer(state, action);
  }
  if (Object.values(resetRemoveAccessActionTypes).includes(action.type)) {
    return resetRemoveAccessReducer(state, action);
  }
  return state;
};
