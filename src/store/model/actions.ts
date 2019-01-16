import { Model } from 'models/Model';
import { createAsyncAction } from 'typesafe-actions';
import MockDataService from '../../services/MockDataService';
import { modelsActionTypes, modelsFetchThunk } from './types';

const internalFetchModel = createAsyncAction(
  modelsActionTypes.FETCH_MODELS_REQUEST,
  modelsActionTypes.FETCH_MODELS_SUCESS,
  modelsActionTypes.FETCH_MODELS_FAILURE
)<void, Model[], void>();

export const fetchModels: modelsFetchThunk = (id: string) => async (dispatch, getState) => {
  dispatch(internalFetchModel.request());

  await new Promise<Model[]>((resolve, reject) => {
    resolve(new MockDataService().getProject(id).Models);
  }).then(res => {
    dispatch(internalFetchModel.success(res));
  });
};
