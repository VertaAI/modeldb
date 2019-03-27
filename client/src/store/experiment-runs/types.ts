import ModelRecord from 'models/ModelRecord';

export interface IExperimentRunsState {
  readonly loading: boolean;
  readonly data?: ModelRecord[] | undefined;
}

export enum fetchExperimentRunsActionTypes {
  FETCH_EXP_RUNS_REQUEST = '@@experiment-runs/FETCH_EXP_RUNS_REQUEST',
  FETCH_EXP_RUNS_SUCCESS = '@@experiment-runs/FETCH_EXP_RUNS_SUCCESS',
  FETCH_EXP_RUNS_FAILURE = '@@experiment-runs/FETCH_EXP_RUNS_FAILURE',
}

export type fetchExperimentRunsAction =
  | { type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_REQUEST }
  | {
      type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_SUCCESS;
      payload?: ModelRecord[];
    }
  | { type: fetchExperimentRunsActionTypes.FETCH_EXP_RUNS_FAILURE };
