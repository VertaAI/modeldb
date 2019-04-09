import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'utils/redux/communication';

export interface ICollaborationState {
  communications: {
    sendingInvitation: ICommunication;
    changingOwner: ICommunication;
    changingAccess: ICommunication;
    removingAccess: ICommunication;
  };
}

export enum InvitationStatus {
  None,
  Sending,
  Success,
  Failure,
}

export const sendInvitationActionTypes = makeCommunicationActionTypes({
  request: '@@collaboration/SEND_INVITATION_REQUEST',
  success: '@@collaboration/SEND_INVITATION_SUCСESS',
  failure: '@@collaboration/SEND_INVITATION_FAILURE',
});
export type ISendInvitationActions = MakeCommunicationActions<
  typeof sendInvitationActionTypes,
  {}
>;
export enum resetInvitationActionTypes {
  RESET_INVITATION_STATE = '@@collaboration/RESET_INVITATION_STATE',
}
export interface IResetInvitationAction {
  type: resetInvitationActionTypes.RESET_INVITATION_STATE;
}

export const changeOwnerActionTypes = makeCommunicationActionTypes({
  request: '@@collaboration/CHANGE_OWNER_REQUEST',
  success: '@@collaboration/CHANGE_OWNER_SUCСESS',
  failure: '@@collaboration/CHANGE_OWNER_FAILURE',
});
export type IChangeOwnerActions = MakeCommunicationActions<
  typeof changeOwnerActionTypes,
  {}
>;
export enum resetChangeOwnerActionTypes {
  RESET_CHANGE_OWNER = '@@collaboration/RESET_CHANGE_OWNER',
}
export interface IResetChangeOwnerAction {
  type: resetChangeOwnerActionTypes.RESET_CHANGE_OWNER;
}

export const changeAccessActionTypes = makeCommunicationActionTypes({
  request: '@@collaboration/CHANGE_ACCESS_REQUEST',
  success: '@@collaboration/CHANGE_ACCESS_SUCСESS',
  failure: '@@collaboration/CHANGE_ACCESS_FAILURE',
});
export type IChangeAccessActions = MakeCommunicationActions<
  typeof changeAccessActionTypes,
  {}
>;
export enum resetChangeAccessActionTypes {
  RESET_CHANGE_ACCESS = '@@collaboration/RESET_CHANGE_ACCESS',
}
export interface IResetChangeAccessAction {
  type: resetChangeAccessActionTypes.RESET_CHANGE_ACCESS;
}

export const removeAccessActionTypes = makeCommunicationActionTypes({
  request: '@@collaboration/REMOVE_ACCESS_REQUEST',
  success: '@@collaboration/REMOVE_ACCESS_SUCСESS',
  failure: '@@collaboration/REMOVE_ACCESS_FAILURE',
});
export type IRemoveAccessActions = MakeCommunicationActions<
  typeof removeAccessActionTypes,
  {}
>;
export enum resetRemoveAccessActionTypes {
  RESET_REMOVE_ACCESS = '@@collaboration/RESET_REMOVE_ACCESS',
}
export interface IResetRemoveAccessAction {
  type: resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS;
}
