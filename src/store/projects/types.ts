import Project, { UserAccess } from 'models/Project';

export enum fetchProjectsActionTypes {
  FETCH_PROJECTS_REQUEST = '@@projects/FETCH_PROJECTS_REQUEST',
  FETCH_PROJECTS_SUCCESS = '@@projects/FETCH_PROJECTS_SUCCESS',
  FETCH_PROJECTS_FAILURE = '@@projects/FETCH_PROJECTS_FAILURE'
}

export type fetchProjectsAction =
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST }
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_SUCCESS; payload: Project[] }
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_FAILURE };

export interface IProjectsState {
  readonly loading: boolean;
  readonly data?: Project[] | null;
}

export enum updateProjectActionTypes {
  UPDATE_PROJECT_STATE = '@@projects/UPDATE_PROJECT_STATE'
}

export interface IUpdateProjectAction {
  type: updateProjectActionTypes.UPDATE_PROJECT_STATE;
  payload: Project[];
}
