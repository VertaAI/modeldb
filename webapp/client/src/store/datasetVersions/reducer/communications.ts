import {
  makeCommunicationReducerFromEnum,
  makeCommunicationReducerByIdFromEnum,
  CommunicationActionsToObj,
} from 'core/shared/utils/redux/communication';
import { combineReducers } from 'redux';

import {
  IDatasetVersionsState,
  loadDatasetVersionsActionTypes,
  IDeleteDatasetVersionActions,
  deleteDatasetVersionActionTypes,
  loadDatasetVersionActionTypes,
  loadComparedDatasetVersionsActionTypes,
  deleteDatasetVersionsActionTypes,
  loadDatasetVersionExperimentRunsActionTypes,
  ILoadDatasetVersionExperimentRunsActions,
} from '../types';

export default combineReducers<IDatasetVersionsState['communications']>({
  loadingDatasetVersions: makeCommunicationReducerFromEnum(
    loadDatasetVersionsActionTypes
  ),
  deletingDatasetVersion: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteDatasetVersionActions,
      typeof deleteDatasetVersionActionTypes
    >,
    string
  >(deleteDatasetVersionActionTypes, {
    request: ({ id }) => id,
    success: ({ id }) => id,
    failure: ({ id }) => id,
  }),
  deletingDatasetVersions: makeCommunicationReducerFromEnum(
    deleteDatasetVersionsActionTypes
  ),
  loadingDatasetVersion: makeCommunicationReducerFromEnum(
    loadDatasetVersionActionTypes
  ),
  loadingComparedDatasetVersions: makeCommunicationReducerFromEnum(
    loadComparedDatasetVersionsActionTypes
  ),
  loadDatasetVersionExperimentRuns: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadDatasetVersionExperimentRunsActions,
      typeof loadDatasetVersionExperimentRunsActionTypes
    >,
    string
  >(loadDatasetVersionExperimentRunsActionTypes, {
    request: ({ datasetVersionId }) => datasetVersionId,
    success: ({ datasetVersionId }) => datasetVersionId,
    failure: ({ datasetVersionId }) => datasetVersionId,
  }),
});
