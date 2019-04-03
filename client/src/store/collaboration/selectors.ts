import { IApplicationState } from '../store';
import { ICollaborationState } from './types';

const selectState = (state: IApplicationState): ICollaborationState =>
  state.collaboration;

export const selectInviteNewCollaborator = (state: IApplicationState) =>
  selectState(state).inviteNewCollaborator;

// todo maybe rename
export const selectAnyError = (state: IApplicationState) => {
  const { changeAccess, changeOwner, removeAccess } = selectState(state);
  return changeAccess.error || changeOwner.error || removeAccess.error;
};
