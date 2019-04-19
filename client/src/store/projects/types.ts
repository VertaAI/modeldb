import { Project } from 'models/Project';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'utils/redux/communication';

export interface IProjectsState {
  data: {
    projects: Project[] | null;
  };
  communications: {
    loadingProjects: ICommunication;
  };
}

export const loadProjectsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/LOAD_PROJECTS_REQUEST',
  SUCCESS: '@@projects/LOAD_PROJECTS_SUCСESS',
  FAILURE: '@@projects/LOAD_PROJECTS_FAILURE',
});
export type ILoadProjectsActions = MakeCommunicationActions<
  typeof loadProjectsActionTypes,
  { success: Project[] }
>;

export enum updateProjectActionTypes {
  UPDATE_PROJECT_STATE = '@@projects/UPDATE_PROJECT_STATE',
}
export interface IUpdateProjectAction {
  type: updateProjectActionTypes.UPDATE_PROJECT_STATE;
  payload: Project[];
}

export enum updateProjectByIdActionTypes {
  UPDATE_PROJECT = '@@projects/UPDATE_PROJECT',
}
export interface IUpdateProjectByIdAction {
  type: updateProjectByIdActionTypes.UPDATE_PROJECT;
  payload: Project;
}

export type FeatureAction =
  | ILoadProjectsActions
  | IUpdateProjectAction
  | IUpdateProjectByIdAction;
