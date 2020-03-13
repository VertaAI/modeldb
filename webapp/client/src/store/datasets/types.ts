import { EntityErrorType } from 'core/shared/models/Common';
import { AppError } from 'core/shared/models/Error';
import { DataWithPagination, IPagination } from 'core/shared/models/Pagination';
import {
  ICommunication,
  ICommunicationById,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';
import { Dataset } from 'models/Dataset';
import { EntityAlreadyExistsErrorType } from 'services/shared/EntityAlreadyExistError';

export interface IDatasetsState {
  data: {
    datasets: Dataset[] | null;
    pagination: IPagination;
    datasetIdsForDeleting: string[];
  };
  communications: {
    creatingDataset: ICommunication<AppError<EntityAlreadyExistsErrorType>>;
    loadingDatasets: ICommunication;
    loadingDataset: ICommunicationById<string, AppError<EntityErrorType>>;
    deletingDataset: ICommunicationById;
    deletingDatasets: ICommunication;
  };
}

export type LoadingDatasetCommunication = ICommunication<
  AppError<EntityErrorType>
>;

export const loadDatasetsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/LOAD_DATASETS_REQUEST',
  SUCCESS: '@@datasets/LOAD_DATASETS_SUCСESS',
  FAILURE: '@@datasets/LOAD_DATASETS_FAILURE',
});
export type ILoadDatasetsActions = MakeCommunicationActions<
  typeof loadDatasetsActionTypes,
  { success: { datasets: DataWithPagination<Dataset> } }
>;

export const deleteDatasetActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/DELETE_DATASET_REQUEST',
  SUCCESS: '@@datasets/DELETE_DATASET_SUCСESS',
  FAILURE: '@@datasets/DELETE_DATASET_FAILURE',
});
export type IDeleteDatasetActions = MakeCommunicationActions<
  typeof deleteDatasetActionTypes,
  {
    request: { id: string };
    success: { id: string };
    failure: { error: AppError; id: string };
  }
>;

export const loadDatasetActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@datasets/LOAD_DATASET_REQUEST',
  SUCCESS: '@@datasets/LOAD_DATASET_SUCСESS',
  FAILURE: '@@datasets/LOAD_DATASET_FAILURE',
});
export type ILoadDatasetActions = MakeCommunicationActions<
  typeof loadDatasetActionTypes,
  {
    request: { id: string };
    success: { dataset: Dataset };
    failure: { id: string; error: AppError };
  }
>;

export enum updateDatasetActionType {
  UPDATE_DATASET = '@@datasets/UPDATE_DATASET',
}
export interface IUpdateDataset {
  type: updateDatasetActionType.UPDATE_DATASET;
  payload: { dataset: Dataset };
}

export enum updateDatasetDescActionType {
  UPDATE_DATASET_DESC = '@@datasets/UPDATE_DATASET_DESC',
}
export interface IUpdateDatasetDesc {
  type: updateDatasetDescActionType.UPDATE_DATASET_DESC;
  payload: { id: string; description: string };
}

export enum updateDatasetTagsActionType {
  UPDATE_DATASET_TAGS = '@@datasets/UPDATE_DATASET_TAGS',
}
export interface IUpdateDatasetTags {
  type: updateDatasetTagsActionType.UPDATE_DATASET_TAGS;
  payload: { datasetId: string; tags: string[] };
}

export enum changeDatasetsPaginationActionType {
  CHANGE_CURRENT_PAGE = '@@datasets/CHANGE_PAGINATION',
}
export interface IChangeDatasetsPagination {
  type: changeDatasetsPaginationActionType.CHANGE_CURRENT_PAGE;
  payload: { currentPage: number };
}

export enum getDefaultDatasetsOptionsActionType {
  GET_DEFAULT_DATASETS_OPTIONS = '@@datasets/GET_DEFAULT_DATASETS_OPTIONS',
}
export interface IGetDefaultDatasetsOptions {
  type: getDefaultDatasetsOptionsActionType.GET_DEFAULT_DATASETS_OPTIONS;
  payload: { options: IDatasetsOptions };
}

export interface IDatasetsOptions {
  paginationCurrentPage?: number;
}

export enum unselectDatasetForDeletingActionType {
  UNSELECT_DATASET_FOR_DELETING = '@@experiments/UNSELECT_DATASET_FOR_DELETING',
}
export interface IUnselectDatasetForDeleting {
  type: unselectDatasetForDeletingActionType.UNSELECT_DATASET_FOR_DELETING;
  payload: { id: string };
}

export enum selectDatasetForDeletingActionType {
  SELECT_DATASET_FOR_DELETING = '@@experiments/SELECT_DATASET_FOR_DELETING',
}
export interface ISelectDatasetForDeleting {
  type: selectDatasetForDeletingActionType.SELECT_DATASET_FOR_DELETING;
  payload: { id: string };
}

export enum resetDatasetsForDeletingActionType {
  RESET_DATASETS_FOR_DELETING = '@@experiments/RESET_DATASETS_FOR_DELETING',
}
export interface IResetDatasetsForDeleting {
  type: resetDatasetsForDeletingActionType.RESET_DATASETS_FOR_DELETING;
}

export const deleteDatasetsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@experiments/DELETE_DATASETS_REQUEST',
  SUCCESS: '@@experiments/DELETE_DATASETS_SUCСESS',
  FAILURE: '@@experiments/DELETE_DATASETS_FAILURE',
});
export type IDeleteDatasetsActions = MakeCommunicationActions<
  typeof deleteDatasetsActionTypes,
  { request: { ids: string[] }; success: { ids: string[] } }
>;

export type FeatureAction =
  | ILoadDatasetsActions
  | IDeleteDatasetActions
  | ILoadDatasetActions
  | IUpdateDataset
  | IUpdateDatasetDesc
  | IUpdateDatasetTags
  | IChangeDatasetsPagination
  | IGetDefaultDatasetsOptions
  | IUnselectDatasetForDeleting
  | ISelectDatasetForDeleting
  | IDeleteDatasetsActions
  | IResetDatasetsForDeleting;
