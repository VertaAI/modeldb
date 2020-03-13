import { EntityErrorType } from 'core/shared/models/Common';
import { AppError } from 'core/shared/models/Error';
import { IPagination, DataWithPagination } from 'core/shared/models/Pagination';
import {
  ICommunication,
  makeCommunicationActionTypes,
  MakeCommunicationActions,
  ICommunicationById,
} from 'core/shared/utils/redux/communication';
import { IDatasetVersion } from 'models/DatasetVersion';
import ModelRecord from 'models/ModelRecord';

export interface IDatasetVersionsState {
  data: {
    datasetVersions: IDatasetVersion[] | null;
    pagination: IPagination;
    datasetVersionIdsForDeleting: string[];
    datasetVersionExperimentRuns: Record<string, ModelRecord[] | undefined>;
  };
  communications: {
    deletingDatasetVersion: ICommunicationById;
    deletingDatasetVersions: ICommunication;
    loadingDatasetVersions: ICommunication;
    loadDatasetVersionExperimentRuns: ICommunicationById;
    loadingDatasetVersion: ICommunication<AppError<EntityErrorType>>;
    loadingComparedDatasetVersions: ICommunication;
  };
}

export const loadDatasetVersionsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/LOAD_DATASET_VERSIONS_REQUEST',
  SUCCESS: '@@datasets/LOAD_DATASET_VERSIONS_SUCСESS',
  FAILURE: '@@datasets/LOAD_DATASET_VERSIONS_FAILURE',
});
export type ILoadDatasetVersionsActions = MakeCommunicationActions<
  typeof loadDatasetVersionsActionTypes,
  {
    request: { datasetId: string };
    success: { datasetVersions: DataWithPagination<IDatasetVersion> };
  }
>;

export const deleteDatasetVersionActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/DELETE_DATASET_VERSION_REQUEST',
  SUCCESS: '@@datasets/DELETE_DATASET_VERSION_SUCСESS',
  FAILURE: '@@datasets/DELETE_DATASET_VERSION_FAILURE',
});
export type IDeleteDatasetVersionActions = MakeCommunicationActions<
  typeof deleteDatasetVersionActionTypes,
  {
    request: { id: string };
    success: { id: string };
    failure: { id: string; error: AppError };
  }
>;

export const loadDatasetVersionActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/LOAD_DATASET_VERSION_REQUEST',
  SUCCESS: '@@datasets/LOAD_DATASET_VERSION_SUCСESS',
  FAILURE: '@@datasets/LOAD_DATASET_VERSION_FAILURE',
});
export type ILoadDatasetVersionActions = MakeCommunicationActions<
  typeof loadDatasetVersionActionTypes,
  { success: { datasetVersion: IDatasetVersion } }
>;

export enum updateDatasetVersionDescActionType {
  UPDATE_DATASET_VERSION_DESC = '@@datasetVersions/UPDATE_DATASET_VERSION_DESC',
}
export interface IUpdateDatasetVersionDesc {
  type: updateDatasetVersionDescActionType.UPDATE_DATASET_VERSION_DESC;
  payload: { id: string; description: string };
}

export enum updateDatasetVersionTagsActionType {
  UPDATE_DATASET_VERSION_TAGS = '@@datasetVersions/UPDATE_DATASET_VERSION_TAGS',
}
export interface IUpdateDatasetVersionTags {
  type: updateDatasetVersionTagsActionType.UPDATE_DATASET_VERSION_TAGS;
  payload: { id: string; tags: string[] };
}

export enum changeDatasetVersionsPaginationActionType {
  CHANGE_CURRENT_PAGE = '@@datasetVersions/CHANGE_PAGINATION',
}
export interface IChangeDatasetVersionsPagination {
  type: changeDatasetVersionsPaginationActionType.CHANGE_CURRENT_PAGE;
  payload: { currentPage: number };
}

export enum getDefaultDatasetVersionsOptionsActionType {
  GET_DEFAULT_DATASET_VERSIONS_OPTIONS = '@@datasetVersions/GET_DEFAULT_DATASETS_OPTIONS',
}
export interface IGetDefaultDatasetVersionsOptions {
  type: getDefaultDatasetVersionsOptionsActionType.GET_DEFAULT_DATASET_VERSIONS_OPTIONS;
  payload: { options: IDatasetVersionsOptions };
}

export interface IDatasetVersionsOptions {
  paginationCurrentPage?: number;
}

export const loadComparedDatasetVersionsActionTypes = makeCommunicationActionTypes(
  {
    REQUEST: '@@datasetVersions/LOAD_COMPARED_DATASET_VERSIONS_REQUEST',
    SUCCESS: '@@datasetVersions/LOAD_COMPARED_DATASET_VERSIONS_SUCСESS',
    FAILURE: '@@datasetVersions/LOAD_COMPARED_DATASET_VERSIONS_FAILURE',
  }
);
export type ILoadComparedDatasetVersionsActions = MakeCommunicationActions<
  typeof loadComparedDatasetVersionsActionTypes,
  {
    request: { datasetVersionId1: string; datasetVersionId2: string };
    success: {
      datasetVersion1: IDatasetVersion;
      datasetVersion2: IDatasetVersion;
    };
  }
>;

export enum unselectDatasetVersionForDeletingActionType {
  UNSELECT_DATASET_VERSION_FOR_DELETING = '@@experiments/UNSELECT_DATASET_VERSION_FOR_DELETING',
}
export interface IUnselectDatasetVersionForDeleting {
  type: unselectDatasetVersionForDeletingActionType.UNSELECT_DATASET_VERSION_FOR_DELETING;
  payload: { id: string };
}

export enum resetDatasetVersionsForDeletingActionType {
  RESET_DATASET_VERSIONS_FOR_DELETING = '@@experiments/RESET_DATASET_VERSIONS_FOR_DELETING',
}
export interface IResetDatasetVersionsForDeleting {
  type: resetDatasetVersionsForDeletingActionType.RESET_DATASET_VERSIONS_FOR_DELETING;
}

export enum selectDatasetVersionForDeletingActionType {
  SELECT_DATASET_VERSION_FOR_DELETING = '@@experiments/SELECT_DATASET_VERSION_FOR_DELETING',
}
export interface ISelectDatasetVersionForDeleting {
  type: selectDatasetVersionForDeletingActionType.SELECT_DATASET_VERSION_FOR_DELETING;
  payload: { id: string };
}
export enum selectAllDatasetVersionsForDeletingActionType {
  SELECT_ALL_DATASET_VERSIONS_FOR_DELETING = '@@experimentRuns/SELECT_ALL_DATASET_VERSIONS_FOR_DELETING',
}
export interface ISelectAllDatasetVersionsForDeleting {
  type: selectAllDatasetVersionsForDeletingActionType.SELECT_ALL_DATASET_VERSIONS_FOR_DELETING;
}

export const deleteDatasetVersionsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/DELETE_DATASET_VERSIONS_REQUEST',
  SUCCESS: '@@experiments/DELETE_DATASET_VERSIONS_SUCСESS',
  FAILURE: '@@experiments/DELETE_DATASET_VERSIONS_FAILURE',
});
export type IDeleteDatasetVersionsActions = MakeCommunicationActions<
  typeof deleteDatasetVersionsActionTypes,
  { request: { ids: string[] }; success: { ids: string[] } }
>;

export const loadDatasetVersionExperimentRunsActionTypes = makeCommunicationActionTypes(
  {
    REQUEST: '@@datasetVersions/LOAD_DATASET_VERSION_EXPERIMENT_RUNS_REQUEST',
    SUCCESS: '@@datasetVersions/LOAD_DATASET_VERSION_EXPERIMENT_RUNS_SUCСESS',
    FAILURE: '@@datasetVersions/LOAD_DATASET_VERSION_EXPERIMENT_RUNS_FAILURE',
  }
);
export type ILoadDatasetVersionExperimentRunsActions = MakeCommunicationActions<
  typeof loadDatasetVersionExperimentRunsActionTypes,
  {
    request: { datasetVersionId: string };
    success: { datasetVersionId: string; experimentRuns: ModelRecord[] };
    failure: { datasetVersionId: string; error: AppError };
  }
>;

export type FeatureAction =
  | ILoadDatasetVersionsActions
  | IDeleteDatasetVersionActions
  | ILoadDatasetVersionActions
  | IChangeDatasetVersionsPagination
  | IGetDefaultDatasetVersionsOptions
  | IUpdateDatasetVersionDesc
  | IUpdateDatasetVersionTags
  | ILoadComparedDatasetVersionsActions
  | IUnselectDatasetVersionForDeleting
  | ISelectDatasetVersionForDeleting
  | ISelectAllDatasetVersionsForDeleting
  | IDeleteDatasetVersionsActions
  | IResetDatasetVersionsForDeleting
  | ILoadDatasetVersionExperimentRunsActions;
