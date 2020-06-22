import { combineReducers } from 'redux';

import {
  makeCommunicationReducerFromEnum,
  makeResetCommunicationReducer,
} from 'shared/utils/redux/communication';
import composeReducers from 'shared/utils/redux/composeReducers';

import {
  createProjectActionTypes,
  IProjectCreationState,
  resetCreateProjectCommunicationActionTypes,
} from '../types';

export default combineReducers<IProjectCreationState['communications']>({
  creatingProject: composeReducers([
    makeCommunicationReducerFromEnum(createProjectActionTypes),
    makeResetCommunicationReducer(
      resetCreateProjectCommunicationActionTypes.RESET_CREATE_PROJECT_COMMUNICATION
    ),
  ]),
});
