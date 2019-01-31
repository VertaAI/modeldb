import ModelRecord from 'models/ModelRecord';

export interface IExperimentRunsState {
  readonly loading: boolean;
  readonly data?: ModelRecord[] | null;
}

export enum fetchExperimentRunsActionTypes {
  FETCH_MODEL_RECORD_REQUEST = '@@model/FETCH_MODEL_RECORD_REQUEST',
  FETCH_MODEL_RECORD_SUCESS = '@@model/FETCH_MODEL_RECORD_SUCESS',
  FETCH_MODEL_RECORD_FAILURE = '@@model/FETCH_MODEL_RECORD_FAILURE'
}

export type fetchExperimentRunsAction =
  | { type: fetchExperimentRunsActionTypes.FETCH_MODEL_RECORD_REQUEST }
  | { type: fetchExperimentRunsActionTypes.FETCH_MODEL_RECORD_SUCESS; payload?: ModelRecord[] }
  | { type: fetchExperimentRunsActionTypes.FETCH_MODEL_RECORD_FAILURE };
