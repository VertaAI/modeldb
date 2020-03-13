import * as R from 'ramda';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';
import cloneClassInstance from 'core/shared/utils/cloneClassInstance';
import { upsert } from 'core/shared/utils/collection';
import ModelRecord from 'models/ModelRecord';

import { ActionType, getType } from 'typesafe-actions';
import * as actions from '../actions';
import {
  FeatureAction,
  IExperimentRunsState,
  loadExperimentRunsActionTypes,
  loadSequentialChartDataActionTypes,
  lazyLoadChartDataActionTypes,
  cleanChartDataPayload,
  updateExpRunTagsActionType,
  updateExpRunDescActionType,
  changePaginationActionType,
  changeSortingActionType,
  loadExperimentRunActionTypes,
  deleteExperimentRunActionTypes,
  getDefaultExperimentRunsSettingsActionType,
  deleteExperimentRunArtifactActionTypes,
  selectExperimentRunForDeletingActionType,
  unselectExperimentRunForDeletingActionType,
  deleteExperimentRunsActionTypes,
  resetExperimentRunsForDeletingActionType,
  selectAllExperimentRunsForDeletingActionType,
} from '../types';

const initial: IExperimentRunsState['data'] = {
  modelRecords: null,
  sorting: null,
  pagination: {
    currentPage: 0,
    pageSize: 10,
    totalCount: 0,
  },
  modelRecordIdsForDeleting: [],
  sequentialChartData: [],
  lazyChartData: [],
  totalCount: 0,
};

const updateExpRunById = (
  f: (modelRecord: ModelRecord) => ModelRecord,
  id: string,
  expRuns: ModelRecord[]
) => {
  return expRuns.map(mr => (mr.id === id ? f(mr) : mr));
};

const dataReducer = (
  state: IExperimentRunsState['data'] = initial,
  action: FeatureAction | ActionType<typeof actions.setExperimentRuns>
): IExperimentRunsState['data'] => {
  switch (action.type) {
    case getType(actions.setExperimentRuns): {
      return {
        ...state,
        modelRecords: action.payload,
      };
    }
    case loadExperimentRunsActionTypes.SUCCESS: {
      return {
        ...state,
        modelRecords: action.payload.experimentRuns,
        pagination: {
          ...state.pagination,
          totalCount: action.payload.totalCount,
        },
      };
    }
    case loadSequentialChartDataActionTypes.SUCCESS: {
      let prevChartData: any;
      if (state.sequentialChartData && state.sequentialChartData.length === 0) {
        prevChartData = state.lazyChartData;
      } else {
        prevChartData = state.sequentialChartData;
      }
      return {
        ...state,
        sequentialChartData: [
          ...prevChartData,
          ...action.payload.sequentialChartData,
        ],
      };
    }
    case lazyLoadChartDataActionTypes.SUCCESS: {
      return {
        ...state,
        lazyChartData: action.payload.lazyChartData,
        totalCount: action.payload.totalCount,
      };
    }
    case cleanChartDataPayload.CLEAN_CHART_DATA: {
      return {
        ...state,
        lazyChartData: [],
        sequentialChartData: [],
        totalCount: 0,
      };
    }
    case loadExperimentRunActionTypes.SUCCESS: {
      return {
        ...state,
        modelRecords: upsert(action.payload, state.modelRecords || []),
      };
    }
    case changePaginationActionType.CHANGE_CURRENT_PAGE: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage: action.payload.currentPage,
        },
      };
    }
    case changeSortingActionType.CHANGE_SORTING: {
      return {
        ...state,
        sorting: action.payload.sorting,
      };
    }
    case updateExpRunTagsActionType.UPDATE_EXPERIMENT_RUN_TAGS: {
      return {
        ...state,
        modelRecords: updateExpRunById(
          modelRecord => {
            const newModelRecord = cloneClassInstance(modelRecord);
            newModelRecord.tags = action.payload.tags;
            return newModelRecord;
          },
          action.payload.id,
          state.modelRecords || []
        ),
      };
    }
    case updateExpRunDescActionType.UPDATE_EXPERIMENT_RUN_DESC: {
      return {
        ...state,
        modelRecords: updateExpRunById(
          modelRecord => {
            const newModelRecord = cloneClassInstance(modelRecord);
            newModelRecord.description = action.payload.description;
            return newModelRecord;
          },
          action.payload.id,
          state.modelRecords || []
        ),
      };
    }
    case deleteExperimentRunActionTypes.SUCCESS: {
      return {
        ...state,
        modelRecords: (state.modelRecords || []).filter(
          ({ id }) => id !== action.payload.id
        ),
        modelRecordIdsForDeleting: R.without(
          [action.payload.id],
          state.modelRecordIdsForDeleting
        ),
        pagination: substractPaginationTotalCount(1, state.pagination),
      };
    }
    case deleteExperimentRunsActionTypes.SUCCESS: {
      return {
        ...state,
        modelRecords: (state.modelRecords || []).filter(
          modelRecord => !action.payload.ids.includes(modelRecord.id)
        ),
        pagination: substractPaginationTotalCount(
          action.payload.ids.length,
          state.pagination
        ),
        modelRecordIdsForDeleting: [],
      };
    }
    case deleteExperimentRunArtifactActionTypes.SUCCESS: {
      return {
        ...state,
        modelRecords: updateExpRunById(
          modelRecord => {
            const newModelRecord = cloneClassInstance(modelRecord);
            newModelRecord.artifacts = modelRecord.artifacts.filter(
              ({ key }) => key !== action.payload.artifactKey
            );
            return newModelRecord;
          },
          action.payload.id,
          state.modelRecords || []
        ),
      };
    }
    case getDefaultExperimentRunsSettingsActionType.GET_DEFAULT_EXPERIMENT_RUNS_SETTINGS: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage:
            (action.payload.options.pagination &&
              action.payload.options.pagination.currentPage) ||
            initial.pagination.currentPage,
        },
        sorting: action.payload.options.sorting || initial.sorting,
      };
    }

    case selectExperimentRunForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING: {
      return {
        ...state,
        modelRecordIdsForDeleting: state.modelRecordIdsForDeleting.concat(
          action.payload.id
        ),
      };
    }
    case unselectExperimentRunForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING: {
      return {
        ...state,
        modelRecordIdsForDeleting: state.modelRecordIdsForDeleting.filter(
          id => action.payload.id !== id
        ),
      };
    }
    case selectAllExperimentRunsForDeletingActionType.SELECT_ALL_EXPERIMENT_RUNS_FOR_DELETING: {
      return {
        ...state,
        modelRecordIdsForDeleting: (state.modelRecords || []).map(
          ({ id }) => id
        ),
      };
    }
    case resetExperimentRunsForDeletingActionType.RESET_EXPERIMENT_RUNS_FOR_DELETING: {
      return {
        ...state,
        modelRecordIdsForDeleting: [],
      };
    }

    default:
      return state;
  }
};

export default dataReducer;
