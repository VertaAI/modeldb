import { Reducer } from 'redux';
import { ActionType, getType } from 'typesafe-actions';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';
import { updateById, upsert } from 'core/shared/utils/collection';
import { Dataset } from 'models/Dataset';

import * as actions from '../actions';
import {
  IDatasetsState,
  FeatureAction,
  loadDatasetsActionTypes,
  loadDatasetActionTypes,
  deleteDatasetActionTypes,
  updateDatasetActionType,
  updateDatasetDescActionType,
  updateDatasetTagsActionType,
  changeDatasetsPaginationActionType,
  getDefaultDatasetsOptionsActionType,
  selectDatasetForDeletingActionType,
  unselectDatasetForDeletingActionType,
  deleteDatasetsActionTypes,
  resetDatasetsForDeletingActionType,
} from '../types';

const initial: IDatasetsState['data'] = {
  datasets: null,
  pagination: {
    currentPage: 0,
    pageSize: 5,
    totalCount: 0,
  },
  datasetIdsForDeleting: [],
};

const updateDatasetById = (
  f: (dataset: Dataset) => Dataset,
  id: string,
  state: IDatasetsState['data']
): IDatasetsState['data'] => {
  return {
    ...state,
    datasets: updateById(f, id, state.datasets || []),
  };
};

const dataReducer: Reducer<
  IDatasetsState['data'],
  FeatureAction | ActionType<typeof actions.createDataset.success>
> = (state = initial, action) => {
  switch (action.type) {
    case getType(actions.createDataset.success): {
      return {
        ...state,
        datasets: (state.datasets || []).concat(action.payload.dataset),
      };
    }
    case loadDatasetsActionTypes.SUCCESS: {
      return {
        ...state,
        datasets: action.payload.datasets.data,
        pagination: {
          ...state.pagination,
          totalCount: action.payload.datasets.totalCount,
        },
      };
    }
    case loadDatasetActionTypes.SUCCESS: {
      return {
        ...state,
        datasets: upsert(action.payload.dataset, state.datasets || []),
      };
    }
    case updateDatasetActionType.UPDATE_DATASET: {
      return updateDatasetById(
        _ => action.payload.dataset,
        action.payload.dataset.id,
        state
      );
    }
    case updateDatasetTagsActionType.UPDATE_DATASET_TAGS: {
      return updateDatasetById(
        dataset => ({ ...dataset, tags: action.payload.tags }),
        action.payload.datasetId,
        state
      );
    }
    case updateDatasetDescActionType.UPDATE_DATASET_DESC: {
      return updateDatasetById(
        dataset => ({ ...dataset, description: action.payload.description }),
        action.payload.id,
        state
      );
    }
    case deleteDatasetActionTypes.SUCCESS: {
      return {
        ...state,
        datasets: (state.datasets || []).filter(
          ({ id }) => action.payload.id !== id
        ),
        pagination: substractPaginationTotalCount(1, state.pagination),
      };
    }
    case deleteDatasetsActionTypes.SUCCESS: {
      return {
        ...state,
        datasets: (state.datasets || []).filter(
          dataset => !action.payload.ids.includes(dataset.id)
        ),
        pagination: substractPaginationTotalCount(
          action.payload.ids.length,
          state.pagination
        ),
        datasetIdsForDeleting: [],
      };
    }
    case changeDatasetsPaginationActionType.CHANGE_CURRENT_PAGE: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage: action.payload.currentPage,
        },
      };
    }
    case getDefaultDatasetsOptionsActionType.GET_DEFAULT_DATASETS_OPTIONS: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage:
            action.payload.options.paginationCurrentPage ||
            initial.pagination.currentPage,
        },
      };
    }

    case selectDatasetForDeletingActionType.SELECT_DATASET_FOR_DELETING: {
      return {
        ...state,
        datasetIdsForDeleting: state.datasetIdsForDeleting.concat(
          action.payload.id
        ),
      };
    }
    case unselectDatasetForDeletingActionType.UNSELECT_DATASET_FOR_DELETING: {
      return {
        ...state,
        datasetIdsForDeleting: state.datasetIdsForDeleting.filter(
          id => action.payload.id !== id
        ),
      };
    }
    case resetDatasetsForDeletingActionType.RESET_DATASETS_FOR_DELETING: {
      return {
        ...state,
        datasetIdsForDeleting: [],
      };
    }

    default:
      return state;
  }
};

export default dataReducer;
