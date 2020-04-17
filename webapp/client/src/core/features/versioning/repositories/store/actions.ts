import { createAction } from 'typesafe-actions';

import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { DataWithPagination, IPagination } from 'core/shared/models/Pagination';
import {
  IRepository,
  IRepositoryNamedIdentification,
  Label,
  RepositoryVisibility,
} from 'core/shared/models/Versioning/Repository';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';
import { ActionResult } from 'store/store';
import * as Actions from 'utils/redux/actions';

import { selectPagination } from './selectors';

export const createRepository = Actions.makeThunkApiRequest(
  '@@repositories/CREATE_REPOSITORY_REQUEST',
  '@@repositories/CREATE_REPOSITORY_SUCCESS',
  '@@repositories/CREATE_REPOSITORY_FAILURE',
  '@@repositories/CREATE_REPOSITORY_RESET'
)<
  {
    repositorySettings: {
      name: IRepository['name'];
    };
    workspaceName: IWorkspace['name'];
  },
  IRepository,
  AppError
>(
  async ({ payload, dependencies: { ServiceFactory } }) =>
    await ServiceFactory.getRepositoriesService().createRepository(payload)
);

export const updateRepository = createAction(
  '@@repositories/UPDATE_REPOSITORY'
)<IRepository>();

export const loadRepositories = Actions.makeThunkApiRequest(
  '@@repositories/LOAD_REPOSITORIES_REQUEST',
  '@@repositories/LOAD_REPOSITORIES_SUCCESS',
  '@@repositories/LOAD_REPOSITORIES_FAILURE',
  '@@repositories/LOAD_REPOSITORIES_RESET'
)<
  { workspaceName: IWorkspace['name'] },
  DataWithPagination<IRepository>,
  AppError
>(async ({ payload, dependencies: { ServiceFactory }, getState }) => {
  const pagination = selectPagination(getState());
  return await ServiceFactory.getRepositoriesService().loadRepositories({
    ...payload,
    pagination,
  });
});
export const changeCurrentPage = createAction(
  '@@repositories/CHANGE_CURRENT_PAGE'
)<number>();

export const loadRepositoryByName = Actions.makeThunkApiRequest(
  '@@repositories/LOAD_REPOSITORY_REQUEST',
  '@@repositories/LOAD_REPOSITORY_SUCCESS',
  '@@repositories/LOAD_REPOSITORY_FAILURE',
  '@@repositories/LOAD_REPOSITORY_RESET'
)<
  IRepositoryNamedIdentification,
  IRepository,
  { name: IRepository['name']; error: AppError<UnavailableEntityApiErrorType> },
  { name: IRepository['name'] }
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getRepositoriesService().loadRepositoryByName(
      payload
    );
  },
  undefined,
  ({ requestPayload, error }) => ({ name: requestPayload.name, error })
);

export const deleteRepository = Actions.makeThunkApiRequest(
  '@@repositories/DELETE_REPOSITORY_REQUEST',
  '@@repositories/DELETE_REPOSITORY_SUCCESS',
  '@@repositories/DELETE_REPOSITORY_FAILURE',
  '@@repositories/DELETE_REPOSITORY_RESET'
)<
  { id: IRepository['id'] },
  { id: IRepository['id'] },
  { id: IRepository['id']; error: AppError<UnavailableEntityApiErrorType> },
  { id: IRepository['id'] }
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    await ServiceFactory.getRepositoriesService().deleteRepository(payload.id);
    return payload;
  },
  undefined,
  ({ requestPayload, error }) => ({ id: requestPayload.id, error })
);

export const loadRepositoriesWithURLPagination = ({
  workspaceName,
}: {
  workspaceName: IWorkspace['name'];
}): ActionResult<void, any> => async dispatch => {
  const queryParams = routes.repositories.parseQueryParams(
    window.location.href
  );
  if (queryParams && queryParams.page) {
    dispatch(changeCurrentPage(Number(queryParams.page) - 1));
  }

  dispatch(loadRepositories({ workspaceName }));
};

export const changePageWithLoadRepositories = ({
  workspaceName,
  page,
}: {
  page: IPagination['currentPage'];
  workspaceName: IWorkspace['name'];
}): ActionResult<void, any> => async dispatch => {
  dispatch(changeCurrentPage(page));
  dispatch(loadRepositories({ workspaceName }));
};

export const addRepositoryLabel = Actions.makeThunkApiRequest(
  '@@repositories/ADD_LABEL_REQUEST',
  '@@repositories/ADD_LABEL_SUCCESS',
  '@@repositories/ADD_LABEL_FAILURE',
  '@@repositories/ADD_LABEL_RESET'
)<
  { repositoryId: IRepository['id']; label: Label },
  { repositoryId: IRepository['id']; label: Label },
  { repositoryId: IRepository['id'] },
  { repositoryId: IRepository['id'] }
>(async ({ payload, dependencies: { ServiceFactory } }) => {
  const { repositoryId, label } = payload;
  await ServiceFactory.getMetaDataService().addRepositoryLabels(
    payload.repositoryId,
    payload.label
  );
  return { repositoryId, label };
});

export const deleteRepositoryLabel = Actions.makeThunkApiRequest(
  '@@repositories/DELETE_LABEL_REQUEST',
  '@@repositories/DELETE_LABEL_SUCCESS',
  '@@repositories/DELETE_LABEL_FAILURE',
  '@@repositories/DELETE_LABEL_RESET'
)<
  { repositoryId: IRepository['id']; label: Label },
  { repositoryId: IRepository['id']; label: Label },
  { repositoryId: IRepository['id'] },
  { repositoryId: IRepository['id'] }
>(async ({ payload, dependencies: { ServiceFactory } }) => {
  const { repositoryId, label } = payload;
  await ServiceFactory.getMetaDataService().deleteRepositoryLabels(
    payload.repositoryId,
    payload.label
  );
  return { repositoryId, label };
});
