import { combineReducers } from 'redux';

import {
  makeCommunicationReducerFromEnum,
  makeResetCommunicationReducer,
  makeCommunicationReducerByIdFromEnum,
  CommunicationActionsToObj,
} from 'core/shared/utils/redux/communication';
import composeReducers from 'core/shared/utils/redux/composeReducers';

import {
  IArtifactManagerState,
  loadArtifactUrlActionTypes,
  downloadArtifactActionTypes,
  loadArtifactPreviewActionTypes,
  loadDatasetVersionActionTypes,
  ILoadDatasetVersionActions,
  resetActionType,
  IDeleteArtifactActions,
  deleteArtifactActionTypes,
} from '../types';

export default combineReducers<IArtifactManagerState['communications']>({
  loadingArtifactUrl: composeReducers([
    makeCommunicationReducerFromEnum(loadArtifactUrlActionTypes),
    makeResetCommunicationReducer(resetActionType.RESET),
  ]),
  downloadingArtifact: composeReducers([
    makeCommunicationReducerFromEnum(downloadArtifactActionTypes),
    makeResetCommunicationReducer(resetActionType.RESET),
  ]),
  loadingArtifactPreview: composeReducers([
    makeCommunicationReducerFromEnum(loadArtifactPreviewActionTypes),
    makeResetCommunicationReducer(resetActionType.RESET),
  ]),
  loadingDatasetVersions: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadDatasetVersionActions,
      typeof loadDatasetVersionActionTypes
    >,
    string
  >(loadDatasetVersionActionTypes, {
    request: ({ datasetVersionId }) => datasetVersionId,
    success: ({ datasetVersionId }) => datasetVersionId,
    failure: ({ datasetVersionId }) => datasetVersionId,
  }),
  deletingArtifact: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteArtifactActions,
      typeof deleteArtifactActionTypes
    >,
    string
  >(deleteArtifactActionTypes, {
    request: ({ experimentRunId }) => experimentRunId,
    success: ({ experimentRunId }) => experimentRunId,
    failure: ({ experimentRunId }) => experimentRunId,
  }),
});
