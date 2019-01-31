import ModelRecord from 'models/ModelRecord';

export interface IExperimentRunsState {
  readonly loading: boolean;
  readonly data?: ModelRecord[] | null;
}

export enum fetchExperimentRunsActionTypes {
  FETCH_EXP_RUNS_REQUEST = '@@experiment-runs/FETCH_EXP_RUNS_REQUEST',
  FETCH_EXP_RUNS_SUCESS = '@@experiment-runs/FETCH_EXP_RUNS_SUCESS',
  FETCH_EXP_RUNS_FAILURE = '@@experiment-runs/FETCH_EXP_RUNS_FAILURE'
}

export type fetchExperimentRunsAction =
  | { type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_REQUEST }
  | { type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_SUCESS; payload?: ModelRecord[] }
  | { type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_FAILURE };
