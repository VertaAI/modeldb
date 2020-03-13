import { combineReducers } from 'redux';

import {
  CommunicationActionsToObj,
  makeCommunicationReducerByIdFromEnum,
  makeCommunicationReducerFromEnum,
} from 'core/shared/utils/redux/communication';

import {
  IProjectsState,
  IDeleteProjectActions,
  loadProjectsActionTypes,
  deleteProjectActionTypes,
  updateReadmeActionTypes,
  loadProjectActionTypes,
  deleteProjectsActionTypes,
  ILoadProjectDatasetsActions,
  loadProjectDatasetsActionTypes,
  ILoadProjectActions,
} from '../types';

export default combineReducers<IProjectsState['communications']>({
  loadingProjects: makeCommunicationReducerFromEnum(loadProjectsActionTypes),
  loadingProject: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadProjectActions,
      typeof loadProjectActionTypes
    >,
    string
  >(loadProjectActionTypes, {
    request: ({ projectId }) => projectId,
    success: ({ project }) => project.id,
    failure: ({ projectId }) => projectId,
  }),
  updatingReadme: makeCommunicationReducerFromEnum(updateReadmeActionTypes),
  deletingProject: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      IDeleteProjectActions,
      typeof deleteProjectActionTypes
    >,
    string
  >(deleteProjectActionTypes, {
    request: id => id,
    success: payload => payload,
    failure: payload => payload.projectId,
  }),
  deletingProjects: makeCommunicationReducerFromEnum(deleteProjectsActionTypes),
  loadingProjectDatasets: makeCommunicationReducerByIdFromEnum<
    CommunicationActionsToObj<
      ILoadProjectDatasetsActions,
      typeof loadProjectDatasetsActionTypes
    >,
    string
  >(loadProjectDatasetsActionTypes, {
    request: ({ projectId }) => projectId,
    success: ({ projectId }) => projectId,
    failure: ({ projectId }) => projectId,
  }),
});
