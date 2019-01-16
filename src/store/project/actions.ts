import Project from 'models/Project';
import { action } from 'typesafe-actions';
import { IProjectsState, ProjectActionTypes } from './types';

export const fetchProjects = () => action(ProjectActionTypes.FETCH_PROJECTS);
export const fetchSuccess = (data: Project[]) => action(ProjectActionTypes.FETCH_SUCCESS, data);
export const addProject = (data: Project) => action(ProjectActionTypes.ADD_PROJECT, data);
export const removeProjects = (ids: number[]) => action(ProjectActionTypes.REMOVE_PROJECTS, ids);
