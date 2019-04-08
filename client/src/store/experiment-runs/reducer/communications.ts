import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import { IExperimentRunsState, loadExperimentRunsActionTypes } from '../types';

export default combineReducers<IExperimentRunsState['communications']>({
  loadingExperimentRuns: makeCommunicationReducerFromEnum(
    loadExperimentRunsActionTypes
  ),
});
