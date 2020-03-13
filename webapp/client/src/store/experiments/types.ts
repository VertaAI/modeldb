import { AppError } from 'core/shared/models/Error';
import { IPagination } from 'core/shared/models/Pagination';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
  ICommunicationById,
} from 'core/shared/utils/redux/communication';
import Experiment from 'models/Experiment';
import { EntityAlreadyExistsErrorType } from 'services/shared/EntityAlreadyExistError';

export interface IExperimentsState {
  data: {
    experiments: Experiment[] | null;
    pagination: IPagination;
    experimentIdsForDeleting: string[];
  };
  communications: {
    creatingExperiment: ICommunication<AppError<EntityAlreadyExistsErrorType>>;
    loadingExpriments: ICommunication;
    deletingExperiment: ICommunicationById;
    deletingExperiments: ICommunication;
  };
}

export enum updateExperimentDescriptionActionType {
  UPDATE_EXPERIMENT_DESCRIPTION = '@@experiments/UPDATE_EXPERIMENT_DESCRIPTION',
}
export interface IUpdateExperimentDescriptionAction {
  type: updateExperimentDescriptionActionType.UPDATE_EXPERIMENT_DESCRIPTION;
  payload: { id: string; description: string };
}

export enum updateExperimentTagsActionType {
  UPDATE_EXPERIMENT_TAGS = '@@experiments/UPDATE_EXPERIMENT_TAGS',
}
export interface IUpdateExperimentTagsAction {
  type: updateExperimentTagsActionType.UPDATE_EXPERIMENT_TAGS;
  payload: { id: string; tags: string[] };
}

export const loadExperimentsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/LOAD_EXPERIMENTS_REQUEST',
  SUCCESS: '@@experiments/LOAD_EXPERIMENTS_SUCСESS',
  FAILURE: '@@experiments/LOAD_EXPERIMENTS_FAILURE',
});
export type ILoadExperimentsActions = MakeCommunicationActions<
  typeof loadExperimentsActionTypes,
  {
    request: { projectId: string };
    success: { experiments: Experiment[]; totalCount: number };
  }
>;

export const deleteExperimentActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/DELETE_EXPERIMENT_REQUEST',
  SUCCESS: '@@experiments/DELETE_EXPERIMENT_SUCСESS',
  FAILURE: '@@experiments/DELETE_EXPERIMENT_FAILURE',
});
export type IDeleteExperimentActions = MakeCommunicationActions<
  typeof deleteExperimentActionTypes,
  {
    request: { id: string };
    success: { id: string };
    failure: { error: AppError; id: string };
  }
>;

export enum changeExperimentsPaginationActionType {
  CHANGE_CURRENT_PAGE = '@@experiment/CHANGE_PAGINATION',
}
export interface IChangeExperimentsPagination {
  type: changeExperimentsPaginationActionType.CHANGE_CURRENT_PAGE;
  payload: { currentPage: number };
}

export enum getDefaultExperimentsOptionsActionType {
  GET_DEFAULT_EXPERIMENTS_OPTIONS = '@@experiments/GET_DEFAULT_EXPERIMENTS_OPTIONS',
}
export interface IGetDefaultExperimentsOptions {
  type: getDefaultExperimentsOptionsActionType.GET_DEFAULT_EXPERIMENTS_OPTIONS;
  payload: { options: IExperimentsOptions };
}

export interface IExperimentsOptions {
  paginationCurrentPage?: number;
}

export enum unselectExperimentForDeletingActionType {
  UNSELECT_EXPERIMENT_RUN_FOR_DELETING = '@@experiments/UNSELECT_EXPERIMENT_RUN_FOR_DELETING',
}
export interface IUnselectExperimentForDeleting {
  type: unselectExperimentForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING;
  payload: { id: string };
}

export enum selectExperimentForDeletingActionType {
  SELECT_EXPERIMENT_RUN_FOR_DELETING = '@@experiments/SELECT_EXPERIMENT_RUN_FOR_DELETING',
}
export interface ISelectExperimentForDeleting {
  type: selectExperimentForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING;
  payload: { id: string };
}

export enum resetExperimentsForDeletingActionType {
  RESET_EXPERIMENTS_FOR_DELETING = '@@experiments/RESET_EXPERIMENTS_FOR_DELETING',
}
export interface IResetExperimentsForDeleting {
  type: resetExperimentsForDeletingActionType.RESET_EXPERIMENTS_FOR_DELETING;
}

export const deleteExperimentsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/DELETE_EXPERIMENT_RUNS_REQUEST',
  SUCCESS: '@@experiments/DELETE_EXPERIMENT_RUNS_SUCСESS',
  FAILURE: '@@experiments/DELETE_EXPERIMENT_RUNS_FAILURE',
});
export type IDeleteExperimentsActions = MakeCommunicationActions<
  typeof deleteExperimentsActionTypes,
  { request: { ids: string[] }; success: { ids: string[] } }
>;

export type FeatureAction =
  | ILoadExperimentsActions
  | IDeleteExperimentActions
  | IUpdateExperimentDescriptionAction
  | IUpdateExperimentTagsAction
  | IChangeExperimentsPagination
  | IGetDefaultExperimentsOptions
  | IUnselectExperimentForDeleting
  | ISelectExperimentForDeleting
  | IDeleteExperimentsActions
  | IResetExperimentsForDeleting;
