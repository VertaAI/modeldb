import { combineReducers, Reducer } from 'redux';

import {
  makeCommunicationReducerFromEnum,
  makeResetCommunicationReducer,
} from 'utils/redux/communication';
import composeReducers from 'utils/redux/composeReducers';

import {
  changeAccessActionTypes,
  changeOwnerActionTypes,
  ICollaborationState,
  removeAccessActionTypes,
  resetChangeAccessActionTypes,
  resetChangeOwnerActionTypes,
  resetInvitationActionTypes,
  resetRemoveAccessActionTypes,
  sendInvitationActionTypes,
  ILoadCollaboratorsWithOwnerActions,
  loadCollaboratorsWithOwnerActionTypes,
} from '../types';

const loadingCollaboratorsWithOwnerReducer: Reducer<
  ICollaborationState['communications']['loadingCollaboratorsWithOwner'],
  ILoadCollaboratorsWithOwnerActions
> = (state = {}, action) => {
  switch (action.type) {
    case loadCollaboratorsWithOwnerActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload.id]: {
          isSuccess: false,
          isRequesting: true,
          error: '',
        },
      };
    }
    case loadCollaboratorsWithOwnerActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload.projectId]: {
          isSuccess: true,
          isRequesting: false,
          error: '',
        },
      };
    }
    case loadCollaboratorsWithOwnerActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.projectId]: {
          error: action.payload.error,
          isSuccess: false,
          isRequesting: false,
        },
      };
    }
    default:
      return state;
  }
};

export default combineReducers<ICollaborationState['communications']>({
  sendingInvitation: composeReducers([
    makeCommunicationReducerFromEnum(sendInvitationActionTypes),
    makeResetCommunicationReducer(
      resetInvitationActionTypes.RESET_INVITATION_STATE
    ),
  ]),
  changingOwner: composeReducers([
    makeCommunicationReducerFromEnum(changeOwnerActionTypes),
    makeResetCommunicationReducer(
      resetChangeOwnerActionTypes.RESET_CHANGE_OWNER
    ),
  ]),
  changingAccess: composeReducers([
    makeCommunicationReducerFromEnum(changeAccessActionTypes),
    makeResetCommunicationReducer(
      resetChangeAccessActionTypes.RESET_CHANGE_ACCESS
    ),
  ]),
  removingAccess: composeReducers([
    makeCommunicationReducerFromEnum(removeAccessActionTypes),
    makeResetCommunicationReducer(
      resetRemoveAccessActionTypes.RESET_REMOVE_ACCESS
    ),
  ]),
  loadingCollaboratorsWithOwner: loadingCollaboratorsWithOwnerReducer,
});
