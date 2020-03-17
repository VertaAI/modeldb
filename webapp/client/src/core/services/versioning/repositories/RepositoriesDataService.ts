import { bind } from 'decko';
import * as R from 'ramda';

import { BaseDataService } from 'core/services/BaseDataService';
import { convertServerRepositoryToClient } from 'core/services/serverModel/Versioning/Repository/converters';
import { IServerRepository } from 'core/services/serverModel/Versioning/Repository/Repository';
import { EntityErrorType } from 'core/shared/models/Common';
import { IPagination, DataWithPagination } from 'core/shared/models/Pagination';
import {
  IRepository,
  IRepositoryNamedIdentification,
} from 'core/shared/models/Versioning/Repository';
import { IWorkspace } from 'models/Workspace';
import { convertClientPaginationToNamespacedServerPagination } from 'core/services/serverModel/Pagination/converters';
import { IAdditionalServerPaginationInRequest } from 'core/services/serverModel/Pagination/Pagination';
import { addHandlingEntityAlrearyExistsErrorToRequestConfig } from 'core/services/shared/EntityAlreadyExistError';

import { MetaDataService } from '../metaData';
import * as UnavailableEntityApiError from '../../shared/UnavailableEntityApiError';
import UsersService from 'core/services/users/UsersService';
import User, { unknownUser } from 'models/User';

type ILoadRepositoriesRequest = IAdditionalServerPaginationInRequest;

const makeLoadRepositoryRequest = (
  pagination: IPagination
): ILoadRepositoriesRequest => {
  return convertClientPaginationToNamespacedServerPagination(pagination);
};

export default class RepositoriesDataService extends BaseDataService {
  constructor() {
    super();
  }

  @bind
  public async createRepository({
    repositorySettings,
    workspaceName,
  }: {
    repositorySettings: { name: IRepository['name'] };
    workspaceName: IWorkspace['name'];
  }): Promise<IRepository> {
    const response = await this.post<IServerRepository>(
      addHandlingEntityAlrearyExistsErrorToRequestConfig({
        url: `/v1/modeldb/versioning/workspaces/${workspaceName}/repositories`,
        data: repositorySettings,
      })
    );

    return convertServerRepositoryToClient({
      serverRepository: response.data,
      labels: [],
      owner: unknownUser,
    });
  }

  @bind
  public async loadRepositories({
    workspaceName,
    pagination,
  }: {
    workspaceName: IWorkspace['name'];
    pagination: IPagination;
  }): Promise<DataWithPagination<IRepository>> {
    const request = makeLoadRepositoryRequest(pagination);

    const response = await this.get<
      { repositories: IServerRepository[]; total_records: number },
      EntityErrorType
    >({
      url: `/v1/modeldb/versioning/workspaces/${workspaceName}/repositories`,
      config: {
        params: request,
      },
    });

    if (!response.data.repositories) {
      return {
        data: [],
        totalCount: 0,
      };
    }

    const metaDataService = new MetaDataService();
    const usersService = new UsersService();

    const labelsData = await Promise.all(
      response.data.repositories.map(repo =>
        metaDataService.loadRepositoryLabel(repo.id)
      )
    );

    const owners = await usersService.loadUsers(
      R.uniq(
        response.data.repositories.map(({ owner }) => owner).filter(Boolean)
      )
    );

    return {
      data: response.data.repositories.map((repo, index) =>
        convertServerRepositoryToClient({
          serverRepository: repo,
          labels: labelsData[index],
          owner: owners.find(({ id }) => repo.owner === id) || unknownUser,
        })
      ),
      totalCount: response.data.total_records
        ? response.data.total_records
        : response.data.repositories.length,
    };
  }

  @bind
  public async loadRepositoryByName(
    ident: IRepositoryNamedIdentification
  ): Promise<IRepository> {
    const response = await this.get<{ repository: IServerRepository }>(
      UnavailableEntityApiError.addHandlingUnavailableEntityErrorToRequestConfig(
        {
          url: `/v1/modeldb/versioning/workspaces/${
            ident.workspaceName
          }/repositories/${ident.name}`,
        }
      )
    );

    const metaDataService = new MetaDataService();
    const usersService = new UsersService();

    const labels = await metaDataService.loadRepositoryLabel(
      response.data.repository.id
    );
    const owner = response.data.repository.owner
      ? await usersService.loadUser(response.data.repository.owner)
      : unknownUser;

    return convertServerRepositoryToClient({
      serverRepository: response.data.repository,
      labels,
      owner,
    });
  }

  @bind
  public async loadRepositoryById(id: IRepository['id']) {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${id}`,
    });

    const metaDataService = new MetaDataService();
    const usersService = new UsersService();

    const labels = await metaDataService.loadRepositoryLabel(
      response.data.repository.id
    );
    const owner = response.data.repository.owner
      ? await usersService.loadUser(response.data.repository.owner)
      : unknownUser;

    return convertServerRepositoryToClient({
      serverRepository: response.data.repository,
      labels,
      owner,
    });
  }

  @bind
  public async deleteRepository(id: IRepository['id']) {
    await this.delete({
      url: `/v1/modeldb/versioning/repositories/${id}`,
    });
  }
}
