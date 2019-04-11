import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import { IProjectsState, loadProjectsActionTypes } from '../types';

export default combineReducers<IProjectsState['communications']>({
  loadingProjects: makeCommunicationReducerFromEnum(loadProjectsActionTypes),
});
