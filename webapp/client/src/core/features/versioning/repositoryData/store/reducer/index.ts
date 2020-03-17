import { combineReducers } from 'redux';
import { getType } from 'typesafe-actions';

import makeResetReducer from 'core/shared/utils/redux/makeResetReducer';

import { resetFeatureState } from '../actions';
import { IRepositoryDataState } from '../types';
import communications from './communications';
import data from './data';

export default makeResetReducer(
  getType(resetFeatureState),
  combineReducers<IRepositoryDataState>({
    data,
    communications,
  })
);
