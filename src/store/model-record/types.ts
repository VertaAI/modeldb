import ModelRecord from 'models/ModelRecord';

export interface IModelRecordState {
  readonly loading: boolean;
  readonly data?: ModelRecord | null;
}

export enum fetchModelRecordActionTypes {
  FETCH_MODEL_RECORD_REQUEST = '@@model_record/FETCH_MODEL_RECORD_REQUEST',
  FETCH_MODEL_RECORD_SUCESS = '@@model_record/FETCH_MODEL_RECORD_SUCESS',
  FETCH_MODEL_RECORD_FAILURE = '@@model_record/FETCH_MODEL_RECORD_FAILURE'
}

export type fetchModelRecordAction =
  | { type: fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST }
  | { type: fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCESS; payload: ModelRecord }
  | { type: fetchModelRecordActionTypes.FETCH_MODEL_RECORD_FAILURE };
