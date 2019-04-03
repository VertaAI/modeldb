import { IApplicationState } from '../store';
import { IProjectsState } from './types';

const selectState = (state: IApplicationState): IProjectsState =>
  state.projects;

export const selectProjects = (state: IApplicationState) =>
  selectState(state).data;

export const selectIsLoadingProjects = (state: IApplicationState) =>
  selectState(state).loading;

export const selectProject = (state: IApplicationState, id: string) =>
  (selectProjects(state) || []).find(project => project.id === id);

// todo maybe rename?
export const checkProjectById = (state: IApplicationState, projectId: string) =>
  (selectProjects(state) || []).some(project => project.id === projectId);
