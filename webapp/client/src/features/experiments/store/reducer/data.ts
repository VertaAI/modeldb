import * as R from 'ramda';
import { Reducer } from 'redux';
import { ActionType, getType } from 'typesafe-actions';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';
import { updateById } from 'core/shared/utils/collection';

import * as actions from '../actions';
import {
  IExperimentsState,
  FeatureAction,
  loadExperimentsActionTypes,
  deleteExperimentActionTypes,
  updateExperimentDescriptionActionType,
  updateExperimentTagsActionType,
  changeExperimentsPaginationActionType,
  getDefaultExperimentsOptionsActionType,
  deleteExperimentsActionTypes,
  selectExperimentForDeletingActionType,
  unselectExperimentForDeletingActionType,
  resetExperimentsForDeletingActionType,
} from '../types';

const initialState: IExperimentsState['data'] = {
  experiments: null,
  pagination: {
    currentPage: 0,
    pageSize: 10,
    totalCount: 0,
  },
  experimentIdsForDeleting: [],
};

const dataReducer: Reducer<
  IExperimentsState['data'],
  FeatureAction | ActionType<typeof actions.createExperiment.success>
> = (state = initialState, action) => {
  switch (action.type) {
    case getType(actions.createExperiment.success): {
      return {
        ...state,
        experiments: (state.experiments || []).concat(
          action.payload.experiment
        ),
      };
    }
    case loadExperimentsActionTypes.SUCCESS: {
      return {
        ...state,
        experiments: action.payload.experiments,
        pagination: {
          ...state.pagination,
          totalCount: action.payload.totalCount,
        },
      };
    }
    case deleteExperimentActionTypes.SUCCESS: {
      return {
        ...state,
        experiments: R.reject(
          ({ id }) => id === action.payload.id,
          state.experiments || []
        ),
        pagination: substractPaginationTotalCount(1, state.pagination),
      };
    }
    case deleteExperimentsActionTypes.SUCCESS: {
      return {
        ...state,
        experiments: (state.experiments || []).filter(
          experiment => !action.payload.ids.includes(experiment.id)
        ),
        pagination: substractPaginationTotalCount(
          action.payload.ids.length,
          state.pagination
        ),
        experimentIdsForDeleting: [],
      };
    }
    case updateExperimentDescriptionActionType.UPDATE_EXPERIMENT_DESCRIPTION: {
      return {
        ...state,
        experiments: updateById(
          experiment => ({
            ...experiment,
            description: action.payload.description,
          }),
          action.payload.id,
          state.experiments || []
        ),
      };
    }
    case updateExperimentTagsActionType.UPDATE_EXPERIMENT_TAGS: {
      return {
        ...state,
        experiments: updateById(
          experiment => ({ ...experiment, tags: action.payload.tags }),
          action.payload.id,
          state.experiments || []
        ),
      };
    }
    case changeExperimentsPaginationActionType.CHANGE_CURRENT_PAGE: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage: action.payload.currentPage,
        },
      };
    }
    case getDefaultExperimentsOptionsActionType.GET_DEFAULT_EXPERIMENTS_OPTIONS: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage:
            action.payload.options.paginationCurrentPage ||
            initialState.pagination.currentPage,
        },
      };
    }

    case selectExperimentForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING: {
      return {
        ...state,
        experimentIdsForDeleting: state.experimentIdsForDeleting.concat(
          action.payload.id
        ),
      };
    }
    case unselectExperimentForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING: {
      return {
        ...state,
        experimentIdsForDeleting: state.experimentIdsForDeleting.filter(
          id => action.payload.id !== id
        ),
      };
    }
    case resetExperimentsForDeletingActionType.RESET_EXPERIMENTS_FOR_DELETING: {
      return {
        ...state,
        experimentIdsForDeleting: [],
      };
    }

    default:
      return state;
  }
};
export default dataReducer;
