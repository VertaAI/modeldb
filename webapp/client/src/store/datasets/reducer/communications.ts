import * as ActionHelpers from 'core/shared/utils/redux/actions';
import {
  makeCommunicationReducerFromEnum,
  makeCommunicationReducerByIdFromEnum,
  CommunicationActionsToObj,
} from 'core/shared/utils/redux/communication';
import { combineReducers } from 'redux';

import * as actions from '../actions';
import {
  IDatasetsState,
  loadDatasetsActionTypes,
  deleteDatasetActionTypes,
  IDeleteDatasetActions,
  loadDatasetActionTypes,
  deleteDatasetsActionTypes,
  ILoadDatasetActions,
} from '../types';

export default combineReducers<IDatasetsState['communications']>({
  loadingDatasets: makeCommunicationReducerFromEnum(loadDatasetsActionTypes),
  deletingDataset: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteDatasetActions,
      typeof deleteDatasetActionTypes
    >,
    string
  >(deleteDatasetActionTypes, {
    request: ({ id }) => id,
    success: ({ id }) => id,
    failure: ({ id }) => id,
  }),
  loadingDataset: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadDatasetActions,
      typeof loadDatasetActionTypes
    >,
    string
  >(loadDatasetActionTypes, {
    request: ({ id }) => id,
    success: ({ dataset: { id } }) => id,
    failure: ({ id }) => id,
  }),
  deletingDatasets: makeCommunicationReducerFromEnum(deleteDatasetsActionTypes),
  creatingDataset: ActionHelpers.makeCommunicationReducerFromResetableAsyncAction(
    actions.createDataset
  ),
});
