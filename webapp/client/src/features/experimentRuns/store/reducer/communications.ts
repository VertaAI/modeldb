import { combineReducers } from 'redux';

import {
  makeCommunicationReducerFromEnum,
  makeCommunicationReducerByIdFromEnum,
  CommunicationActionsToObj,
} from 'shared/utils/redux/communication';

import {
  IExperimentRunsState,
  ILoadExperimentRunActions,
  loadExperimentRunActionTypes,
  loadExperimentRunsActionTypes,
  loadSequentialChartDataActionTypes,
  lazyLoadChartDataActionTypes,
  IDeleteExperimentRunActions,
  deleteExperimentRunActionTypes,
  IDeleteExperimentRunArtifactActions,
  deleteExperimentRunArtifactActionTypes,
  deleteExperimentRunsActionTypes,
} from '../types';

export default combineReducers<IExperimentRunsState['communications']>({
  loadingExperimentRuns: makeCommunicationReducerFromEnum(
    loadExperimentRunsActionTypes
  ),
  loadingSequentialChartData: makeCommunicationReducerFromEnum(
    loadSequentialChartDataActionTypes
  ),
  loadingLazyChartData: makeCommunicationReducerFromEnum(
    lazyLoadChartDataActionTypes
  ),
  loadingExperimentRun: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadExperimentRunActions,
      typeof loadExperimentRunActionTypes
    >,
    string
  >(loadExperimentRunActionTypes, {
    request: (id) => id,
    success: ({ id }) => id,
    failure: ({ id }) => id,
  }),
  deletingExperimentRun: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteExperimentRunActions,
      typeof deleteExperimentRunActionTypes
    >,
    string
  >(deleteExperimentRunActionTypes, {
    request: ({ id }) => id,
    success: ({ id }) => id,
    failure: ({ id }) => id,
  }),
  deletingExperimentRuns: makeCommunicationReducerFromEnum(
    deleteExperimentRunsActionTypes
  ),
  deletingExperimentRunArtifact: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteExperimentRunArtifactActions,
      typeof deleteExperimentRunArtifactActionTypes
    >,
    string
  >(deleteExperimentRunArtifactActionTypes, {
    request: ({ id, artifactKey }) => `${id}-${artifactKey}`,
    success: ({ id, artifactKey }) => `${id}-${artifactKey}`,
    failure: ({ id, artifactKey }) => `${id}-${artifactKey}`,
  }),
});
