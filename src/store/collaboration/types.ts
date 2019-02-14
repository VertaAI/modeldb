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
