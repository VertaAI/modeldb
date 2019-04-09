import ModelRecord from 'models/ModelRecord';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
  makeCommunicationReducerFromEnum,
} from 'utils/redux/communication';

export interface IExperimentRunsState {
  data: {
    modelRecords: ModelRecord[] | null;
  };
  communications: {
    loadingExperimentRuns: ICommunication;
  };
}

export const loadExperimentRunsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_REQUEST',
  SUCCESS: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_SUCСESS',
  FAILURE: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_FAILURE',
});
export type ILoadExperimentRunsActions = MakeCommunicationActions<
  typeof loadExperimentRunsActionTypes,
  { success: ModelRecord[] }
>;
export const loadExperimentRunsReducer = makeCommunicationReducerFromEnum(
  loadExperimentRunsActionTypes
);

export type FeatureAction = ILoadExperimentRunsActions;
