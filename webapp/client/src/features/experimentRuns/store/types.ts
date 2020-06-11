import { EntityErrorType } from 'core/shared/models/Common';
import { AppError } from 'core/shared/models/Error';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
  makeCommunicationReducerFromEnum,
  ICommunicationById,
} from 'core/shared/utils/redux/communication';
import ModelRecord from 'models/ModelRecord';

export interface IExperimentRunsState {
  data: {
    modelRecords: ModelRecord[] | null;
    pagination: IPagination;
    sorting: ISorting | null;
    modelRecordIdsForDeleting: string[];
    lazyChartData: ModelRecord[] | null;
    sequentialChartData: ModelRecord[] | null;
    totalCount: number;
  };
  communications: {
    loadingExperimentRuns: ICommunication;
    loadingLazyChartData: ICommunication;
    loadingSequentialChartData: ICommunication;
    loadingExperimentRun: ICommunicationById<string, AppError<EntityErrorType>>;
    deletingExperimentRun: ICommunicationById;
    deletingExperimentRuns: ICommunication;
    deletingExperimentRunArtifact: ICommunicationById;
  };
}

export enum changeSortingActionType {
  CHANGE_SORTING = '@@experimentRuns/CHANGE_SORTING',
}
export interface IChangeSorting {
  type: changeSortingActionType.CHANGE_SORTING;
  payload: { sorting: ISorting | null };
}

export enum changePaginationActionType {
  CHANGE_CURRENT_PAGE = '@@experimentRuns/CHANGE_PAGINATION',
}
export interface IChangePagination {
  type: changePaginationActionType.CHANGE_CURRENT_PAGE;
  payload: { currentPage: number };
}

export const loadExperimentRunsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_REQUEST',
  SUCCESS: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_SUCСESS',
  FAILURE: '@@experimentRuns/LOAD_EXPERIMENT_RUNS_FAILURE',
});
export interface ISuccessLoadExperimentRunsPayload {
  experimentRuns: ModelRecord[];
  totalCount: number;
}
export type ILoadExperimentRunsActions = MakeCommunicationActions<
  typeof loadExperimentRunsActionTypes,
  { success: ISuccessLoadExperimentRunsPayload }
>;

export const lazyLoadChartDataActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/LAZY_LOAD_CHART_DATA_REQUEST',
  SUCCESS: '@@experimentRuns/LAZY_LOAD_CHART_DATA_SUCСESS',
  FAILURE: '@@experimentRuns/LAZY_LOAD_CHART_DATA_FAILURE',
});
export interface ISuccessLazyLoadChartDataPayload {
  lazyChartData: ModelRecord[];
  totalCount: number;
}
export type ILazyLoadChartDataActions = MakeCommunicationActions<
  typeof lazyLoadChartDataActionTypes,
  { success: ISuccessLazyLoadChartDataPayload }
>;
export const loadSequentialChartDataActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/LOAD_SEQUENTIAL_CHART_DATA_REQUEST',
  SUCCESS: '@@experimentRuns/LOAD_SEQUENTIAL_CHART_DATA_SUCСESS',
  FAILURE: '@@experimentRuns/LOAD_SEQUENTIAL_CHART_DATA_FAILURE',
});
export interface ISuccessLoadSequentialChartDataPayload {
  sequentialChartData: ModelRecord[];
}
export type ILoadSequentialChartDataActions = MakeCommunicationActions<
  typeof loadSequentialChartDataActionTypes,
  { success: ISuccessLoadSequentialChartDataPayload }
>;

export enum cleanChartDataPayload {
  CLEAN_CHART_DATA = '@@experimentRuns/CLEAN_CHART_DATA',
}
export interface ICleanChartDataPayload {
  type: cleanChartDataPayload.CLEAN_CHART_DATA;
}

export const loadExperimentRunActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/LOAD_EXPERIMENT_RUN_REQUEST',
  SUCCESS: '@@experimentRuns/LOAD_EXPERIMENT_RUN_SUCСESS',
  FAILURE: '@@experimentRuns/LOAD_EXPERIMENT_RUN_FAILURE',
});
export type ILoadExperimentRunActions = MakeCommunicationActions<
  typeof loadExperimentRunActionTypes,
  {
    request: string;
    success: ModelRecord;
    failure: { id: string; error: AppError };
  }
>;

export enum updateExpRunTagsActionType {
  UPDATE_EXPERIMENT_RUN_TAGS = '@@experimentRuns/UPDATE_EXPERIMENT_RUN_TAGS',
}
export interface IUpdateExpRunTags {
  type: updateExpRunTagsActionType.UPDATE_EXPERIMENT_RUN_TAGS;
  payload: { id: string; tags: string[] };
}

export enum updateExpRunDescActionType {
  UPDATE_EXPERIMENT_RUN_DESC = '@@experimentRuns/UPDATE_EXPERIMENT_RUN_DESC',
}
export interface IUpdateExpRunDesc {
  type: updateExpRunDescActionType.UPDATE_EXPERIMENT_RUN_DESC;
  payload: { id: string; description: string };
}

export const deleteExperimentRunActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/DELETE_EXPERIMENT_RUN_REQUEST',
  SUCCESS: '@@experimentRuns/DELETE_EXPERIMENT_RUN_SUCСESS',
  FAILURE: '@@experimentRuns/DELETE_EXPERIMENT_RUN_FAILURE',
});
export type IDeleteExperimentRunActions = MakeCommunicationActions<
  typeof deleteExperimentRunActionTypes,
  {
    request: { id: string };
    success: { id: string };
    failure: { id: string; error: AppError };
  }
>;
export const deleteExperimentRunReducer = makeCommunicationReducerFromEnum(
  deleteExperimentRunActionTypes
);

export interface IExprRunsOptions {
  pagination?: { currentPage: number };
  sorting?: ISorting;
}

export enum getDefaultExperimentRunsSettingsActionType {
  GET_DEFAULT_EXPERIMENT_RUNS_SETTINGS = '@@experimentRuns/GET_DEFAULT_EXPERIMENT_RUNS_SETTINGS',
}
export interface IGetDefaultExperimentRunsSettings {
  type: getDefaultExperimentRunsSettingsActionType.GET_DEFAULT_EXPERIMENT_RUNS_SETTINGS;
  payload: { options: IExprRunsOptions };
}

export enum resetExperimentRunsSettingsActionType {
  RESET_EXPERIMENT_RUNS_SETTINGS = '@@experimentRuns/RESET_EXPERIMENT_RUNS_SETTINGS',
}
export interface IResetExperimentRunsSettings {
  type: resetExperimentRunsSettingsActionType.RESET_EXPERIMENT_RUNS_SETTINGS;
}

export const deleteExperimentRunArtifactActionTypes = makeCommunicationActionTypes(
  {
    REQUEST: '@@experimentRuns/DELETE_EXPERIMENT_RUN_ARTIFACT_REQUEST',
    SUCCESS: '@@experimentRuns/DELETE_EXPERIMENT_RUN_ARTIFACT_SUCСESS',
    FAILURE: '@@experimentRuns/DELETE_EXPERIMENT_RUN_ARTIFACT_FAILURE',
  }
);
export type IDeleteExperimentRunArtifactActions = MakeCommunicationActions<
  typeof deleteExperimentRunArtifactActionTypes,
  {
    request: { id: string; artifactKey: string };
    success: { id: string; artifactKey: string };
    failure: { id: string; artifactKey: string; error: AppError };
  }
>;

export enum unselectExperimentRunForDeletingActionType {
  UNSELECT_EXPERIMENT_RUN_FOR_DELETING = '@@experimentRuns/UNSELECT_EXPERIMENT_RUN_FOR_DELETING',
}
export interface IUnselectExperimentRunForDeleting {
  type: unselectExperimentRunForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING;
  payload: { id: string };
}

export enum selectExperimentRunForDeletingActionType {
  SELECT_EXPERIMENT_RUN_FOR_DELETING = '@@experimentRuns/SELECT_EXPERIMENT_RUN_FOR_DELETING',
}
export interface ISelectExperimentRunForDeleting {
  type: selectExperimentRunForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING;
  payload: { id: string };
}

export enum selectAllExperimentRunsForDeletingActionType {
  SELECT_ALL_EXPERIMENT_RUNS_FOR_DELETING = '@@experimentRuns/SELECT_ALL_EXPERIMENT_RUNS_FOR_DELETING',
}
export interface ISelectAllExperimentRunsForDeleting {
  type: selectAllExperimentRunsForDeletingActionType.SELECT_ALL_EXPERIMENT_RUNS_FOR_DELETING;
}

export enum resetExperimentRunsForDeletingActionType {
  RESET_EXPERIMENT_RUNS_FOR_DELETING = '@@experiments/RESET_EXPERIMENT_RUNS_FOR_DELETING',
}
export interface IResetExperimentRunsForDeleting {
  type: resetExperimentRunsForDeletingActionType.RESET_EXPERIMENT_RUNS_FOR_DELETING;
}

export const deleteExperimentRunsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experimentRuns/DELETE_EXPERIMENT_RUNS_REQUEST',
  SUCCESS: '@@experimentRuns/DELETE_EXPERIMENT_RUNS_SUCСESS',
  FAILURE: '@@experimentRuns/DELETE_EXPERIMENT_RUNS_FAILURE',
});
export type IDeleteExperimentRunsActions = MakeCommunicationActions<
  typeof deleteExperimentRunsActionTypes,
  { request: { ids: string[] }; success: { ids: string[] } }
>;

export type FeatureAction =
  | ILoadExperimentRunsActions
  | ILoadSequentialChartDataActions
  | ICleanChartDataPayload
  | ILazyLoadChartDataActions
  | ILoadExperimentRunActions
  | IUpdateExpRunTags
  | IUpdateExpRunDesc
  | IChangePagination
  | IChangeSorting
  | IDeleteExperimentRunActions
  | IGetDefaultExperimentRunsSettings
  | IDeleteExperimentRunArtifactActions
  | IUnselectExperimentRunForDeleting
  | IDeleteExperimentRunsActions
  | ISelectExperimentRunForDeleting
  | IResetExperimentRunsForDeleting
  | IResetExperimentRunsSettings
  | ISelectAllExperimentRunsForDeleting;
