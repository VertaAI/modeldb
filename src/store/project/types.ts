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
