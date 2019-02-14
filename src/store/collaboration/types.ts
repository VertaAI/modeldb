interface ISendInvitationState {
  readonly sending: boolean;
  readonly result?: boolean;
}

export interface ICollaborationState {
  readonly changeOwner: ISendInvitationState;
  readonly inviteNewCollaborator: ISendInvitationState;
}

export enum sendInvitationActionTypes {
  SEND_INVITATION_REQUEST = '@@collaboration/SEND_INVITATION_REQUEST',
  SEND_INVITATION_SUCCESS = '@@collaboration/SEND_INVITATION_SUCCESS',
  SEND_INVITATION_FAILURE = '@@collaboration/SEND_INVITATION_FAILURE'
}

export type sendInvitationAction =
  | { type: sendInvitationActionTypes.SEND_INVITATION_REQUEST }
  | { type: sendInvitationActionTypes.SEND_INVITATION_SUCCESS; payload: boolean }
  | { type: sendInvitationActionTypes.SEND_INVITATION_FAILURE };

export enum changeOwnerActionTypes {
  CHANGE_OWNER_REQUEST = '@@collaboration/CHANGE_OWNER_REQUEST',
  CHANGE_OWNER_SUCCESS = '@@collaboration/CHANGE_OWNER_SUCCESS',
  CHANGE_OWNER_FAILURE = '@@collaboration/CHANGE_OWNER_FAILURE'
}

export type changeOwnerAction =
  | { type: changeOwnerActionTypes.CHANGE_OWNER_REQUEST }
  | { type: changeOwnerActionTypes.CHANGE_OWNER_SUCCESS; payload: boolean }
  | { type: changeOwnerActionTypes.CHANGE_OWNER_FAILURE };

export enum resetInvitationActionTypes {
  RESET_INVITATION_STATE = '@@collaboration/RESET_INVITATION_STATE'
}

export interface IResetInvitationAction {
  type: resetInvitationActionTypes.RESET_INVITATION_STATE;
}

export enum resetChangeOwnerActionTypes {
  RESET_CHANGE_OWNER = '@@collaboration/RESET_CHANGE_OWNER'
}

export interface IResetChangeOwnerAction {
  type: resetChangeOwnerActionTypes.RESET_CHANGE_OWNER;
}

export enum changeAccessActionTypes {
  CHANGE_ACCESS_REQUEST = '@@collaboration/CHANGE_ACCESS_REQUEST',
  CHANGE_ACCESS_SUCCESS = '@@collaboration/CHANGE_ACCESS_SUCCESS',
  CHANGE_ACCESS_FAILURE = '@@collaboration/CHANGE_ACCESS_FAILURE'
}

export type changeAccessAction =
  | { type: changeAccessActionTypes.CHANGE_ACCESS_REQUEST }
  | { type: changeAccessActionTypes.CHANGE_ACCESS_SUCCESS; payload: boolean }
  | { type: changeAccessActionTypes.CHANGE_ACCESS_FAILURE };

export enum removeAccessActionTypes {
  REMOVE_ACCESS_REQUEST = '@@collaboration/REMOVE_ACCESS_REQUEST',
  REMOVE_ACCESS_SUCCESS = '@@collaboration/REMOVE_ACCESS_SUCCESS',
  REMOVE_ACCESS_FAILURE = '@@collaboration/REMOVE_ACCESS_FAILURE'
}

export type removeAccessAction =
  | { type: removeAccessActionTypes.REMOVE_ACCESS_REQUEST }
  | { type: removeAccessActionTypes.REMOVE_ACCESS_SUCCESS; payload: boolean }
  | { type: removeAccessActionTypes.REMOVE_ACCESS_FAILURE };
