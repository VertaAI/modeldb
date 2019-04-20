import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';

export interface ILocationState {
  projectId?: string;
  runId?: string;
}

export enum projectFetchActionTypes {
  GET_PROJECT_REQUEST = '@@location/GET_PROJECT_REQUEST',
  GET_PROJECT_SUCCESS = '@@location/GET_PROJECT_SUCСESS',
  GET_PROJECT_FAILURE = '@@location/GET_PROJECT_FAILURE',
}
export type projectFetchAction =
  | { type: projectFetchActionTypes.GET_PROJECT_REQUEST }
  | {
      type: projectFetchActionTypes.GET_PROJECT_SUCCESS;
      payload: Project;
    }
  | { type: projectFetchActionTypes.GET_PROJECT_FAILURE };

export enum runFetchActionTypes {
  GET_RUN_REQUEST = '@@location/GET_RUN_REQUEST',
  GET_RUN_SUCCESS = '@@location/GET_RUN_SUCСESS',
  GET_RUN_FAILURE = '@@location/GET_RUN_FAILURE',
}
export type runFetchAction =
  | { type: runFetchActionTypes.GET_RUN_REQUEST }
  | {
      type: runFetchActionTypes.GET_RUN_SUCCESS;
      payload: ModelRecord;
    }
  | { type: runFetchActionTypes.GET_RUN_FAILURE };
