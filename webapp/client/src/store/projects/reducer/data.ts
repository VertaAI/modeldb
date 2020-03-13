import * as R from 'ramda';
import { Reducer } from 'redux';
import { ActionType, getType } from 'typesafe-actions';

import cloneClassInstance from 'core/shared/utils/cloneClassInstance';
import { upsert } from 'core/shared/utils/collection';
import { Project } from 'models/Project';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';
import * as actions from '../actions';
import {
  FeatureAction,
  IProjectsState,
  loadProjectsActionTypes,
  deleteProjectActionTypes,
  updateProjectActionType,
  updateReadmeActionTypes,
  updateProjectTagsActionType,
  updateProjectDescActionType,
  loadProjectActionTypes,
  changeProjectsPaginationActionType,
  getDefaultProjectsOptionsActionType,
  deleteProjectsActionTypes,
  selectProjectForDeletingActionType,
  unselectProjectForDeletingActionType,
  loadProjectDatasetsActionTypes,
  resetProjectsForDeletingActionType,
} from '../types';

export const initial: IProjectsState['data'] = {
  projectsDatasets: {},

  projects: null,
  pagination: {
    currentPage: 0,
    pageSize: 10,
    totalCount: 0,
  },
  projectIdsForDeleting: [],
};

const updateProjectById = (
  f: (project: Project) => Project,
  id: string,
  projects: Project[]
) => {
  return projects.map(p => (p.id === id ? f(p) : p));
};

const dataReducer: Reducer<
  IProjectsState['data'],
  FeatureAction | ActionType<typeof actions.setProjects>
> = (state = initial, action) => {
  switch (action.type) {
    case getType(actions.setProjects): {
      return {
        ...state,
        projects: action.payload,
      };
    }
    case loadProjectsActionTypes.SUCCESS: {
      return {
        ...state,
        projects: action.payload.projects.data,
        pagination: {
          ...state.pagination,
          totalCount: action.payload.projects.totalCount,
        },
      };
    }
    case loadProjectActionTypes.SUCCESS: {
      return {
        ...state,
        projects: upsert(action.payload.project, state.projects || []),
      };
    }
    case updateProjectActionType.UPDATE_PROJECT: {
      return {
        ...state,
        projects: updateProjectById(
          _ => action.payload,
          action.payload.id,
          state.projects || []
        ),
      };
    }
    case deleteProjectActionTypes.SUCCESS: {
      return {
        ...state,
        projects: R.reject(
          ({ id }) => action.payload === id,
          state.projects || []
        ),
        pagination: substractPaginationTotalCount(1, state.pagination),
      };
    }
    case deleteProjectsActionTypes.SUCCESS: {
      return {
        ...state,
        projects: (state.projects || []).filter(
          project => !action.payload.ids.includes(project.id)
        ),
        pagination: substractPaginationTotalCount(
          action.payload.ids.length,
          state.pagination
        ),
        projectIdsForDeleting: [],
      };
    }
    case updateReadmeActionTypes.SUCCESS: {
      return {
        ...state,
        projects: updateProjectById(
          project => {
            const newProject = cloneClassInstance(project);
            newProject.readme = action.payload.readme;
            return newProject;
          },
          action.payload.projectId,
          state.projects || []
        ),
      };
    }
    case updateProjectTagsActionType.UPDATE_PROJECT_TAGS: {
      return {
        ...state,
        projects: updateProjectById(
          project => {
            const newProject = cloneClassInstance(project);
            newProject.tags = action.payload.tags;
            return newProject;
          },
          action.payload.projectId,
          state.projects || []
        ),
      };
    }
    case updateProjectDescActionType.UPDATE_PROJECT_DESC: {
      return {
        ...state,
        projects: updateProjectById(
          project => {
            const newProject = cloneClassInstance(project);
            newProject.description = action.payload.description;
            return newProject;
          },
          action.payload.projectId,
          state.projects || []
        ),
      };
    }

    case changeProjectsPaginationActionType.CHANGE_CURRENT_PAGE: {
      return {
        ...state,
        pagination: {
          ...state.pagination,
          currentPage: action.payload.currentPage,
        },
      };
    }
    case getDefaultProjectsOptionsActionType.GET_DEFAULT_PROJECTS_OPTIONS: {
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

    case selectProjectForDeletingActionType.SELECT_PROJECT_FOR_DELETING: {
      return {
        ...state,
        projectIdsForDeleting: state.projectIdsForDeleting.concat(
          action.payload.id
        ),
      };
    }
    case unselectProjectForDeletingActionType.UNSELECT_PROJECT_FOR_DELETING: {
      return {
        ...state,
        projectIdsForDeleting: state.projectIdsForDeleting.filter(
          id => action.payload.id !== id
        ),
      };
    }
    case resetProjectsForDeletingActionType.RESET_PROJECTS_FOR_DELETING: {
      return {
        ...state,
        projectIdsForDeleting: [],
      };
    }

    case loadProjectDatasetsActionTypes.SUCCESS: {
      return {
        ...state,
        projectsDatasets: {
          ...state.projectsDatasets,
          [action.payload.projectId]: action.payload.datasets,
        },
      };
    }
    default:
      return state;
  }
};

export default dataReducer;
