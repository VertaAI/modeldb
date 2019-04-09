import { IApplicationState } from '../store';
import { ICollaborationState, InvitationStatus } from './types';

const selectState = (state: IApplicationState): ICollaborationState =>
  state.collaboration;

export const selectInviteNewCollaboratorInfo = (state: IApplicationState) => {
  const invitingNewCollaborator = selectCommunications(state).sendingInvitation;
  const invitationStatus = (() => {
    if (invitingNewCollaborator.error) {
      return InvitationStatus.Failure;
    }
    if (invitingNewCollaborator.isSuccess) {
      return InvitationStatus.Success;
    }
    if (invitingNewCollaborator.isRequesting) {
      return InvitationStatus.Sending;
    }
    return InvitationStatus.None;
  })();
  return { error: invitingNewCollaborator.error, status: invitationStatus };
};

// todo maybe rename
export const selectAnyError = (state: IApplicationState) => {
  const {
    changingAccess,
    changingOwner,
    removingAccess,
  } = selectCommunications(state);
  return changingAccess.error || changingOwner.error || removingAccess.error;
};

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;
