import * as R from 'ramda';
import { Reducer } from 'redux';

import { IDatasetVersion } from 'models/DatasetVersion';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';
import { updateById, upsert } from 'core/shared/utils/collection';
import {
  IDatasetVersionsState,
  FeatureAction,
  loadDatasetVersionsActionTypes,
  deleteDatasetVersionActionTypes,
  loadDatasetVersionActionTypes,
  changeDatasetVersionsPaginationActionType,
  getDefaultDatasetVersionsOptionsActionType,
  updateDatasetVersionDescActionType,
  updateDatasetVersionTagsActionType,
  loadComparedDatasetVersionsActionTypes,
  selectDatasetVersionForDeletingActionType,
  unselectDatasetVersionForDeletingActionType,
  deleteDatasetVersionsActionTypes,
  resetDatasetVersionsForDeletingActionType,
  selectAllDatasetVersionsForDeletingActionType,
  loadDatasetVersionExperimentRunsActionTypes,
} from '../types';

const initial: IDatasetVersionsState['data'] = {
  datasetVersions: null,
  pagination: {
    currentPage: 0,
    pageSize: 20,
    totalCount: 0,
  },
  datasetVersionIdsForDeleting: [],
  datasetVersionExperimentRuns: {},
};

const updateDatasetVersionById = (
  f: (dataset: IDatasetVersion) => IDatasetVersion,
  id: string,
  state: IDatasetVersionsState['data']
): IDatasetVersionsState['data'] => {
  return {
    ...state,
    datasetVersions: updateById(f, id, state.datasetVersions || []),
  };
};

const dataReducer: Reducer<IDatasetVersionsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadDatasetVersionsActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: action.payload.datasetVersions.data,
        pagination: {
          ...state.pagination,
          totalCount: action.payload.datasetVersions.totalCount,
        },
      };
    }
    case loadDatasetVersionActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: upsert(
          action.payload.datasetVersion,
          state.datasetVersions || []
        ),
      };
    }
    case deleteDatasetVersionActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: (state.datasetVersions || []).filter(
          datasetVersion => datasetVersion.id !== action.payload.id
        ),
        datasetVersionIdsForDeleting: R.without(
          [action.payload.id],
          state.datasetVersionIdsForDeleting
        ),
        pagination: substractPaginationTotalCount(1, state.pagination),
      };
    }
    case deleteDatasetVersionsActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: (state.datasetVersions || []).filter(
          project => !action.payload.ids.includes(project.id)
        ),
        pagination: substractPaginationTotalCount(
          action.payload.ids.length,
          state.pagination
        ),
        datasetVersionIdsForDeleting: [],
      };
    }
    case updateDatasetVersionDescActionType.UPDATE_DATASET_VERSION_DESC: {
      return updateDatasetVersionById(
        datasetVersion => ({
          ...datasetVersion,
          description: action.payload.description,
        }),
        action.payload.id,
        state
      );
    }
    case updateDatasetVersionTagsActionType.UPDATE_DATASET_VERSION_TAGS: {
      return updateDatasetVersionById(
        datasetVersion => ({
          ...datasetVersion,
          tags: action.payload.tags,
        }),
        action.payload.id,
        state
      );
    }

    case changeDatasetVersionsPaginationActionType.CHANGE_CURRENT_PAGE: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage: action.payload.currentPage,
        },
      };
    }
    case getDefaultDatasetVersionsOptionsActionType.GET_DEFAULT_DATASET_VERSIONS_OPTIONS: {
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

    case loadComparedDatasetVersionsActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: [
          action.payload.datasetVersion1,
          action.payload.datasetVersion2,
        ],
      };
    }

    case selectDatasetVersionForDeletingActionType.SELECT_DATASET_VERSION_FOR_DELETING: {
      return {
        ...state,
        datasetVersionIdsForDeleting: state.datasetVersionIdsForDeleting.concat(
          action.payload.id
        ),
      };
    }
    case selectAllDatasetVersionsForDeletingActionType.SELECT_ALL_DATASET_VERSIONS_FOR_DELETING: {
      return {
        ...state,
        datasetVersionIdsForDeleting: (state.datasetVersions || []).map(
          ({ id }) => id
        ),
      };
    }
    case unselectDatasetVersionForDeletingActionType.UNSELECT_DATASET_VERSION_FOR_DELETING: {
      return {
        ...state,
        datasetVersionIdsForDeleting: state.datasetVersionIdsForDeleting.filter(
          id => action.payload.id !== id
        ),
      };
    }
    case resetDatasetVersionsForDeletingActionType.RESET_DATASET_VERSIONS_FOR_DELETING: {
      return {
        ...state,
        datasetVersionIdsForDeleting: [],
      };
    }

    case loadDatasetVersionExperimentRunsActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersionExperimentRuns: {
          ...state.datasetVersionExperimentRuns,
          [action.payload.datasetVersionId]: action.payload.experimentRuns,
        },
      };
    }
    default:
      return state;
  }
};

export default dataReducer;
