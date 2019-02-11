export interface ISendInvitationState {
  readonly sending: boolean;
  readonly result?: boolean;
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

export enum resetInvitationActionTypes {
  RESET_INVITATION_STATE = '@@collaboration/RESET_INVITATION_STATE'
}

export interface IResetInvitationAction {
  type: resetInvitationActionTypes.RESET_INVITATION_STATE;
}
