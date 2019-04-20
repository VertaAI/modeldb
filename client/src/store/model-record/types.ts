import ModelRecord from 'models/ModelRecord';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'utils/redux/communication';

export interface IModelRecordState {
  data: {
    modelRecord: ModelRecord | null;
  };
  communications: {
    loadingModelRecord: ICommunication;
  };
}

export const loadModelRecordActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@modelRecord/LOAD_MODEL_RECORD_REQUEST',
  SUCCESS: '@@modelRecord/LOAD_MODEL_RECORD_SUCСESS',
  FAILURE: '@@modelRecord/LOAD_MODEL_RECORD_FAILURE',
});
export type ILoadModelRecordActions = MakeCommunicationActions<
  typeof loadModelRecordActionTypes,
  { success: ModelRecord }
>;

export type FeatureAction = ILoadModelRecordActions;
