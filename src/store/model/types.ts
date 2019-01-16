import { Model } from 'models/Model';
import { ActionCreator } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { IApplicationState } from 'store/store';

export enum modelsActionTypes {
  FETCH_MODELS_REQUEST = '@@models/FETCH_MODELS_REQUEST',
  FETCH_MODELS_SUCESS = '@@models/FETCH_MODELS_SUCESS',
  FETCH_MODELS_FAILURE = '@@models/FETCH_MODELS_FAILURE'
}

export interface IModelsState {
  readonly loading: boolean;
  readonly data?: Model[] | null;
}

export type modelsFetchAction =
  | { type: modelsActionTypes.FETCH_MODELS_REQUEST }
  | { type: modelsActionTypes.FETCH_MODELS_SUCESS; payload: Model[] }
  | { type: modelsActionTypes.FETCH_MODELS_FAILURE };

export type modelsFetchThunk = ActionCreator<ThunkAction<void, IApplicationState, undefined, modelsFetchAction>>;
