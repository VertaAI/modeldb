import Project from 'models/Project';
import {action} from 'typesafe-actions';
import {IProjectState, projectActionTypes} from './types';

export const fetchProjects = () => action(projectActionTypes.FETCH_PROJECTS);
export const fetchSuccess = (data: Project[]) => action(projectActionTypes.FETCH_SUCCESS, data);
export const addProject = (data: Project) => action(projectActionTypes.ADD_PROJECT, data);
export const removeProjects = (ids: number[]) => action(projectActionTypes.REMOVE_PROJECTS, ids);
