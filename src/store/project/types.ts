import Project from 'models/Project';

export enum ProjectActionTypes {
  ADD_PROJECT = '@@project/ADD_PROJECT',
  FETCH_PROJECTS = '@@project/FETCH_PROJECTS',
  FETCH_SUCCESS = '@@project/FETCH_SUCCESS',
  REMOVE_PROJECTS = '@@project/REMOVE_PROJECTS'
}

export interface IProjectsState {
  readonly loading: boolean;
  readonly data?: Project[] | null;
}

export interface IProjectState {
  readonly loading: boolean;
  readonly data?: Project | null;
}

export enum projectFetchModelsActionTypes {
  FETCH_MODELS_REQUEST = '@@project/FETCH_MODELS_REQUEST',
  FETCH_MODELS_SUCESS = '@@project/FETCH_MODELS_SUCESS',
  FETCH_MODELS_FAILURE = '@@project/FETCH_MODELS_FAILURE'
}

export type projectFetchModelsAction =
  | { type: projectFetchModelsActionTypes.FETCH_MODELS_REQUEST }
  | { type: projectFetchModelsActionTypes.FETCH_MODELS_SUCESS; payload: Project }
  | { type: projectFetchModelsActionTypes.FETCH_MODELS_FAILURE };
