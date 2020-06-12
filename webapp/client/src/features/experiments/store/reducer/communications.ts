import { combineReducers } from 'redux';

import * as ActionHelpers from 'core/shared/utils/redux/actions';
import {
  makeCommunicationReducerFromEnum,
  makeCommunicationReducerByIdFromEnum,
  CommunicationActionsToObj,
} from 'core/shared/utils/redux/communication';

import * as actions from '../actions';
import {
  IExperimentsState,
  loadExperimentsActionTypes,
  deleteExperimentActionTypes,
  IDeleteExperimentActions,
  deleteExperimentsActionTypes,
} from '../types';

export default combineReducers<IExperimentsState['communications']>({
  creatingExperiment: ActionHelpers.makeCommunicationReducerFromResetableAsyncAction(
    actions.createExperiment
  ),
  loadingExpriments: makeCommunicationReducerFromEnum(
    loadExperimentsActionTypes
  ),
  deletingExperiment: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteExperimentActions,
      typeof deleteExperimentActionTypes
    >,
    string
  >(deleteExperimentActionTypes, {
    request: payload => payload.id,
    success: payload => payload.id,
    failure: payload => payload.id,
  }),
  deletingExperiments: makeCommunicationReducerFromEnum(
    deleteExperimentsActionTypes
  ),
});
