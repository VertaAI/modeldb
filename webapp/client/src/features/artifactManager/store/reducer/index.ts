import { combineReducers } from 'redux';

import makeResetReducer from 'core/shared/utils/redux/makeResetReducer';

import { IArtifactManagerState, resetActionType } from '../types';
import communications from './communications';
import data from './data';

export default makeResetReducer(
  resetActionType.RESET,
  combineReducers<IArtifactManagerState>({
    data,
    communications,
  })
);
