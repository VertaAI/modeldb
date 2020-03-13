import {
  initialCommunication,
  ICommunication,
} from 'core/shared/utils/redux/communication';
import { makeSelectDeletingEntity } from 'store/shared/deletion';

import { IApplicationState } from '../store';
import { IProjectsState } from './types';

const selectState = (state: IApplicationState): IProjectsState =>
  state.projects;

export const selectProjects = (state: IApplicationState) =>
  selectState(state).data.projects;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectProject = (state: IApplicationState, id: string) => {
  return (selectProjects(state) || []).find(project => project.id === id);
};

export const selectProjectIdsForDeleting = (state: IApplicationState) => {
  return selectState(state).data.projectIdsForDeleting;
};

export const selectProjectCommunication = (
  state: IApplicationState,
  projectId: string
): ICommunication<any> => {
  const project = selectProject(state, projectId);
  if (project) {
    return { error: undefined, isRequesting: false, isSuccess: true };
  }
  return selectLoadingProject(state, projectId);
};

export const selectDeletingProject = makeSelectDeletingEntity({
  selectBulkDeleting: state => selectCommunications(state).deletingProjects,
  selectEntityDeleting: (state, id) =>
    selectCommunications(state).deletingProject[id],
  selectEntityIdsForDeleting: selectProjectIdsForDeleting,
});

export const selectProjectsPagination = (state: IApplicationState) => {
  return selectState(state).data.pagination;
};

export const selectProjectDatasets = (
  state: IApplicationState,
  projectId: string
) => {
  return selectState(state).data.projectsDatasets[projectId] || null;
};

export const selectLoadingProjectDatasets = (
  state: IApplicationState,
  projectId: string
) =>
  selectCommunications(state).loadingProjectDatasets[projectId] ||
  initialCommunication;

export const selectLoadingProject = (state: IApplicationState, id: string) => {
  if (selectProject(state, id)) {
    return { error: undefined, isRequesting: false, isSuccess: true };
  }
  return selectCommunications(state).loadingProject[id] || initialCommunication;
};
