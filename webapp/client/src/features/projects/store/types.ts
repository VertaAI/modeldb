import { EntityErrorType } from 'core/shared/models/Common';
import { AppError } from 'core/shared/models/Error';
import { DataWithPagination, IPagination } from 'core/shared/models/Pagination';
import {
  ICommunication,
  ICommunicationById,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';
import { Markdown } from 'core/shared/utils/types';
import { Dataset } from 'models/Dataset';
import { Project } from 'models/Project';

export interface IProjectsState {
  data: {
    projects: Project[] | null;
    pagination: IPagination;
    projectIdsForDeleting: string[];

    projectsDatasets: Record<string, Dataset[] | undefined>;
  };
  communications: {
    loadingProjects: ICommunication;
    loadingProject: ICommunicationById<string, AppError<EntityErrorType>>;
    deletingProject: ICommunicationById;
    deletingProjects: ICommunication;
    updatingReadme: ICommunication;

    loadingProjectDatasets: ICommunicationById;
  };
}

export const loadProjectActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/LOAD_PROJECT_REQUEST',
  SUCCESS: '@@projects/LOAD_PROJECT_SUCСESS',
  FAILURE: '@@projects/LOAD_PROJECT_FAILURE',
});
export type ILoadProjectActions = MakeCommunicationActions<
  typeof loadProjectActionTypes,
  {
    request: { projectId: string };
    success: { project: Project };
    failure: { projectId: string; error: AppError };
  }
>;

export const loadProjectsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/LOAD_PROJECTS_REQUEST',
  SUCCESS: '@@projects/LOAD_PROJECTS_SUCСESS',
  FAILURE: '@@projects/LOAD_PROJECTS_FAILURE',
});
export type ILoadProjectsActions = MakeCommunicationActions<
  typeof loadProjectsActionTypes,
  { success: { projects: DataWithPagination<Project> } }
>;

export const deleteProjectActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/REMOVE_PROJECT_REQUEST',
  SUCCESS: '@@projects/REMOVE_PROJECT_SUCСESS',
  FAILURE: '@@projects/REMOVE_PROJECT_FAILURE',
});
export type IDeleteProjectActions = MakeCommunicationActions<
  typeof deleteProjectActionTypes,
  {
    request: string;
    success: string;
    failure: { projectId: string; error: AppError };
  }
>;

export enum updateProjectActionType {
  UPDATE_PROJECT = '@@projects/UPDATE_PROJECT',
}
export interface IUpdateProjectAction {
  type: updateProjectActionType.UPDATE_PROJECT;
  payload: Project;
}

export const updateReadmeActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/UPDATE_README_REQUEST',
  SUCCESS: '@@projects/UPDATE_README_SUCСESS',
  FAILURE: '@@projects/UPDATE_README_FAILURE',
});
export type IUpdateReadmeActions = MakeCommunicationActions<
  typeof updateReadmeActionTypes,
  {
    request: { projectId: string; readme: Markdown };
    success: { projectId: string; readme: Markdown };
  }
>;

export enum updateProjectTagsActionType {
  UPDATE_PROJECT_TAGS = '@@projects/UPDATE_PROJECT_PAGE',
}
export interface IUpdateProjectTags {
  type: updateProjectTagsActionType.UPDATE_PROJECT_TAGS;
  payload: { projectId: string; tags: string[] };
}

export enum updateProjectDescActionType {
  UPDATE_PROJECT_DESC = '@@projects/UPDATE_PROJECT_DESC',
}
export interface IUpdateProjectDesc {
  type: updateProjectDescActionType.UPDATE_PROJECT_DESC;
  payload: { projectId: string; description: string };
}

export enum changeProjectsPaginationActionType {
  CHANGE_CURRENT_PAGE = '@@projects/CHANGE_PAGINATION',
}
export interface IChangeProjectsPagination {
  type: changeProjectsPaginationActionType.CHANGE_CURRENT_PAGE;
  payload: { currentPage: number };
}

export enum getDefaultProjectsOptionsActionType {
  GET_DEFAULT_PROJECTS_OPTIONS = '@@projects/GET_DEFAULT_PROJECTS_OPTIONS',
}
export interface IGetDefaultProjectsOptions {
  type: getDefaultProjectsOptionsActionType.GET_DEFAULT_PROJECTS_OPTIONS;
  payload: { options: IProjectsOptions };
}

export interface IProjectsOptions {
  paginationCurrentPage?: number;
}

export enum unselectProjectForDeletingActionType {
  UNSELECT_PROJECT_FOR_DELETING = '@@experiments/UNSELECT_PROJECT_FOR_DELETING',
}
export interface IUnselectProjectForDeleting {
  type: unselectProjectForDeletingActionType.UNSELECT_PROJECT_FOR_DELETING;
  payload: { id: string };
}

export enum selectProjectForDeletingActionType {
  SELECT_PROJECT_FOR_DELETING = '@@experiments/SELECT_PROJECT_FOR_DELETING',
}
export interface ISelectProjectForDeleting {
  type: selectProjectForDeletingActionType.SELECT_PROJECT_FOR_DELETING;
  payload: { id: string };
}

export enum resetProjectsForDeletingActionType {
  RESET_PROJECTS_FOR_DELETING = '@@experiments/RESET_PROJECTS_FOR_DELETING',
}
export interface IResetProjectsForDeleting {
  type: resetProjectsForDeletingActionType.RESET_PROJECTS_FOR_DELETING;
}

export const deleteProjectsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/DELETE_PROJECTS_REQUEST',
  SUCCESS: '@@experiments/DELETE_PROJECTS_SUCСESS',
  FAILURE: '@@experiments/DELETE_PROJECTS_FAILURE',
});
export type IDeleteProjectsActions = MakeCommunicationActions<
  typeof deleteProjectsActionTypes,
  { request: { ids: string[] }; success: { ids: string[] } }
>;

export const loadProjectDatasetsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projects/LOAD_PROJECT_DATASETS_REQUEST',
  SUCCESS: '@@projects/LOAD_PROJECT_DATASETS_SUCСESS',
  FAILURE: '@@projects/LOAD_PROJECT_DATASETS_FAILURE',
});
export type ILoadProjectDatasetsActions = MakeCommunicationActions<
  typeof loadProjectDatasetsActionTypes,
  {
    request: { projectId: string };
    success: { projectId: string; datasets: Dataset[] };
    failure: { projectId: string; error: AppError };
  }
>;

export type FeatureAction =
  | ILoadProjectsActions
  | ILoadProjectActions
  | IUpdateProjectAction
  | IUpdateProjectAction
  | IDeleteProjectActions
  | IUpdateReadmeActions
  | IUpdateProjectTags
  | IUpdateProjectDesc
  | IChangeProjectsPagination
  | IGetDefaultProjectsOptions
  | IDeleteProjectsActions
  | ISelectProjectForDeleting
  | IUnselectProjectForDeleting
  | ILoadProjectDatasetsActions
  | IResetProjectsForDeleting;
