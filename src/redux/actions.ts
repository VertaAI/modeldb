import { Action } from 'redux';
import { Project } from '../models/Project';

export enum ProjectActionName {
  LOAD_PROJECTS= 'LOAD_PROJECTS',
  ADD_PROJECT= 'ADD_PROJECT',
  REMOVE_PROJECTS= 'REMOVE_PROJECTS',
}

export interface IProjectsLoadAction extends Action<ProjectActionName> {
  type: ProjectActionName.LOAD_PROJECTS;
  projects: Project[];
}

export interface IProjectsAddAction extends Action<ProjectActionName> {
  type: ProjectActionName.ADD_PROJECT;
  project: Project;
}

export type ProjectAction = IProjectsLoadAction | IProjectsAddAction;
