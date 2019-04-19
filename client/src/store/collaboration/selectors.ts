import { ICommunication } from 'utils/redux/communication';

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
  return (
    selectErrorFromMultipleComm(changingAccess) ||
    changingOwner.error ||
    removingAccess.error
  );
};

const selectErrorFromMultipleComm = (
  multipleComm: Record<string, ICommunication>
) => {
  return (
    Object.values(multipleComm)
      .map(comm => comm.error)
      .find(Boolean) || ''
  );
};

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectIsChangingUserAccess = (
  state: IApplicationState,
  userId: string
) => {
  const comm = selectCommunications(state).changingAccess[userId];
  return Boolean(comm && comm.isRequesting);
};

export const selectIsRemovingUserAccess = (
  state: IApplicationState,
  userId: string
) => {
  const comm = selectCommunications(state).removingAccess[userId];
  return Boolean(comm && comm.isRequesting);
};

export const selectIsLoadingProjectCollaboratorsWithOwner = (
  state: IApplicationState,
  projectId: string
) => {
  const comm = selectCommunications(state).loadingCollaboratorsWithOwner[
    projectId
  ];
  return comm && comm.isRequesting;
};
