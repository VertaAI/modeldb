import { Model } from 'models/Model';

export interface IModelState {
  readonly loading: boolean;
  readonly data?: Model | null;
}

export enum fetchModelActionTypes {
  FETCH_MODEL_REQUEST = '@@model/FETCH_MODEL_REQUEST',
  FETCH_MODEL_SUCESS = '@@model/FETCH_MODEL_SUCESS',
  FETCH_MODEL_FAILURE = '@@model/FETCH_MODEL_FAILURE'
}

export type fetchModelAction =
  | { type: fetchModelActionTypes.FETCH_MODEL_REQUEST }
  | { type: fetchModelActionTypes.FETCH_MODEL_SUCESS; payload: Model }
  | { type: fetchModelActionTypes.FETCH_MODEL_FAILURE };
