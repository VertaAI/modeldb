export enum InvitationStatus {
  None,
  Sending,
  Success,
  Failure,
}

interface IInvitationState {
  readonly status: InvitationStatus;
  // should be changed to actual type after getting format from the backend
  readonly error?: any;
}

export interface ICollaborationState {
  readonly changeOwner: IInvitationState;
  readonly inviteNewCollaborator: IInvitationState;
  readonly changeAccess: IInvitationState;
  readonly removeAccess: IInvitationState;
}

export enum sendInvitationActionTypes {
  SEND_INVITATION_REQUEST = '@@collaboration/SEND_INVITATION_REQUEST',
  SEND_INVITATION_SUCCESS = '@@collaboration/SEND_INVITATION_SUCCESS',
  SEND_INVITATION_FAILURE = '@@collaboration/SEND_INVITATION_FAILURE',
}

export type sendInvitationAction =
  | { type: sendInvitationActionTypes.SEND_INVITATION_REQUEST }
  | { type: sendInvitationActionTypes.SEND_INVITATION_SUCCESS }
  // should be changed to actual type after getting format from the backend
  | { type: sendInvitationActionTypes.SEND_INVITATION_FAILURE; payload?: any };

export enum resetInvitationActionTypes {
  RESET_INVITATION_STATE = '@@collaboration/RESET_INVITATION_STATE',
}

export interface IResetInvitationAction {
  type: resetInvitationActionTypes.RESET_INVITATION_STATE;
}

export enum changeOwnerActionTypes {
  CHANGE_OWNER_REQUEST = '@@collaboration/CHANGE_OWNER_REQUEST',
  CHANGE_OWNER_SUCCESS = '@@collaboration/CHANGE_OWNER_SUCCESS',
  CHANGE_OWNER_FAILURE = '@@collaboration/CHANGE_OWNER_FAILURE',
}

export type changeOwnerAction =
  | { type: changeOwnerActionTypes.CHANGE_OWNER_REQUEST }
  | { type: changeOwnerActionTypes.CHANGE_OWNER_SUCCESS }
  // should be changed to actual type after getting format from the backend
  | { type: changeOwnerActionTypes.CHANGE_OWNER_FAILURE; payload?: any };

export enum resetChangeOwnerActionTypes {
  RESET_CHANGE_OWNER = '@@collaboration/RESET_CHANGE_OWNER',
}

export interface IResetChangeOwnerAction {
  type: resetChangeOwnerActionTypes.RESET_CHANGE_OWNER;
}

export enum changeAccessActionTypes {
  CHANGE_ACCESS_REQUEST = '@@collaboration/CHANGE_ACCESS_REQUEST',
  CHANGE_ACCESS_SUCCESS = '@@collaboration/CHANGE_ACCESS_SUCCESS',
  CHANGE_ACCESS_FAILURE = '@@collaboration/CHANGE_ACCESS_FAILURE',
}

export type changeAccessAction =
  | { type: changeAccessActionTypes.CHANGE_ACCESS_REQUEST }
  | { type: changeAccessActionTypes.CHANGE_ACCESS_SUCCESS }
  // should be changed to actual type after getting format from the backend
  | { type: changeAccessActionTypes.CHANGE_ACCESS_FAILURE; payload?: any };

export enum resetChangeAccessActionTypes {
  RESET_CHANGE_ACCESS = '@@collaboration/RESET_CHANGE_ACCESS',
}

export interface IResetChangeAccessAction {
  type: resetChangeAccessActionTypes.RESET_CHANGE_ACCESS;
}

export enum removeAccessActionTypes {
  REMOVE_ACCESS_REQUEST = '@@collaboration/REMOVE_ACCESS_REQUEST',
  REMOVE_ACCESS_SUCCESS = '@@collaboration/REMOVE_ACCESS_SUCCESS',
  REMOVE_ACCESS_FAILURE = '@@collaboration/REMOVE_ACCESS_FAILURE',
}

export type removeAccessAction =
  | { type: removeAccessActionTypes.REMOVE_ACCESS_REQUEST }
  | { type: removeAccessActionTypes.REMOVE_ACCESS_SUCCESS }
  // should be changed to actual type after getting format from the backend
  | { type: removeAccessActionTypes.REMOVE_ACCESS_FAILURE; payload?: any };

export enum resetRemoveAccessActionTypes {
  RESET_REMOVE_ACCESS = '@@collaboration/RESET_REMOVE_ACCESS',
}

export interface IResetRemoveAccessAction {
  type: resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS;
}
