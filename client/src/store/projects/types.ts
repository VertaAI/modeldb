import { Project } from 'models/Project';
import {
  ICommunication,
  makeCommunicationActionTypes,
  MakeCommunicationActions,
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
  request: '@@projects/LOAD_PROJECTS_REQUEST',
  success: '@@projects/LOAD_PROJECTS_SUCСESS',
  failure: '@@projects/LOAD_PROJECTS_FAILURE',
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

export type FeatureAction = ILoadProjectsActions | IUpdateProjectAction;
