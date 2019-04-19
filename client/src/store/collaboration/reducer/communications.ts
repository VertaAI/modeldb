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
  IChangeAccessActions,
  IRemoveAccessActions,
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

const changingAccessReducer: Reducer<
  ICollaborationState['communications']['changingAccess'],
  IChangeAccessActions
> = (state = {}, action) => {
  switch (action.type) {
    case changeAccessActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: {
          isSuccess: false,
          isRequesting: true,
          error: '',
        },
      };
    }
    case changeAccessActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: {
          isSuccess: true,
          isRequesting: false,
          error: '',
        },
      };
    }
    case changeAccessActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.userId]: {
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

const removingAccessReducer: Reducer<
  ICollaborationState['communications']['removingAccess'],
  IRemoveAccessActions
> = (state = {}, action) => {
  switch (action.type) {
    case removeAccessActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: {
          isSuccess: false,
          isRequesting: true,
          error: '',
        },
      };
    }
    case removeAccessActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: {
          isSuccess: true,
          isRequesting: false,
          error: '',
        },
      };
    }
    case removeAccessActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.userId]: {
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
  changingAccess: changingAccessReducer,
  removingAccess: removingAccessReducer,
  loadingCollaboratorsWithOwner: loadingCollaboratorsWithOwnerReducer,
});
