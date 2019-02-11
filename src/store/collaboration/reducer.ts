import { Reducer } from 'redux';
import {
  IResetInvitationAction,
  ISendInvitationState,
  resetInvitationActionTypes,
  sendInvitationAction,
  sendInvitationActionTypes
} from './types';

const sendInvitationInitialState: ISendInvitationState = {
  sending: true
};

const sendInvitationReducer: Reducer<ISendInvitationState> = (state = sendInvitationInitialState, action: sendInvitationAction) => {
  switch (action.type) {
    case sendInvitationActionTypes.SEND_INVITATION_REQUEST: {
      return { ...state, sending: true };
    }
    case sendInvitationActionTypes.SEND_INVITATION_SUCCESS: {
      return { ...state, sending: false, result: true };
    }
    case sendInvitationActionTypes.SEND_INVITATION_FAILURE: {
      return { ...state, sending: false, result: false };
    }
    default: {
      return state;
    }
  }
};

const resetInvitationReducer: Reducer<ISendInvitationState> = (state = sendInvitationInitialState, action: IResetInvitationAction) => {
  switch (action.type) {
    case resetInvitationActionTypes.RESET_INVITATION_STATE: {
      return { ...state, sending: false, result: undefined };
    }
    default: {
      return state;
    }
  }
};

export const collaborationReducer: Reducer<ISendInvitationState> = (state = sendInvitationInitialState, action) => {
  if (Object.values(sendInvitationActionTypes).includes(action.type)) {
    return sendInvitationReducer(state, action);
  }
  if (Object.values(resetInvitationActionTypes).includes(action.type)) {
    return resetInvitationReducer(state, action);
  }
  return state;
};
