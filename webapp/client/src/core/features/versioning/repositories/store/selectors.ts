import { IRepository } from 'core/shared/models/Versioning/Repository';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import { IApplicationState } from 'store/store';

const selectFeatureState = (state: IApplicationState) => state.repositories;

export const selectCommunications = (state: IApplicationState) =>
  selectFeatureState(state).communications;

export const selectRepositories = (state: IApplicationState) =>
  selectFeatureState(state).data.repositories;

export const selectPagination = (state: IApplicationState) =>
  selectFeatureState(state).data.pagination;

export const selectRepositoryById = (
  state: IApplicationState,
  repositoryId: IRepository['id']
) => {
  return (selectRepositories(state) || []).find(r => r.id === repositoryId);
};

export const selectRepositoryByName = (
  state: IApplicationState,
  name: IRepository['name']
) => {
  return (selectRepositories(state) || []).find(r => r.name === name);
};

export const selectAddingRepositoryLabelCommunication = (
  state: IApplicationState,
  repositoryId: IRepository['id']
) => {
  return (
    selectCommunications(state).addingRepositoryLabel[repositoryId] ||
    initialCommunication
  );
};

export const selectDeletingRepositoryLabelCommunication = (
  state: IApplicationState,
  repositoryId: IRepository['id']
) => {
  return (
    selectCommunications(state).deletingRepositoryLabel[repositoryId] ||
    initialCommunication
  );
};
