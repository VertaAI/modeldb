import Project from 'models/Project';

export enum fetchProjectsActionTypes {
  FETCH_PROJECTS_REQUEST = '@@projects/FETCH_PROJECTS_REQUEST',
  FETCH_PROJECTS_SUCESS = '@@projects/FETCH_PROJECTS_SUCESS',
  FETCH_PROJECTS_FAILURE = '@@projects/FETCH_PROJECTS_FAILURE'
}

export type fetchProjectsAction =
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST }
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_SUCESS; payload: Project[] }
  | { type: fetchProjectsActionTypes.FETCH_PROJECTS_FAILURE };

export interface IProjectsState {
  readonly loading: boolean;
  readonly data?: Project[] | null;
}
