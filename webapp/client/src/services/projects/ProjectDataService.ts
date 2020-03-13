import * as R from 'ramda';

import { IArtifact } from 'core/shared/models/Artifact';
import { EntityErrorType } from 'core/shared/models/Common';
import { IFilterData } from 'core/features/filter/Model';
import { DataWithPagination, IPagination } from 'core/shared/models/Pagination';
import { Markdown } from 'core/shared/utils/types';
import {
  Project,
  IProjectCreationSettings,
  projectAlreadyExistsError,
} from 'models/Project';
import { IWorkspace } from 'models/Workspace';
import {
  convertHydratedProjectToClient,
  convertProjectVisibilityToServer,
} from 'services/serverModel/Projects/converters';

import { BaseDataService } from 'core/services/BaseDataService';
import makeLoadProjectsRequest from './responseRequest/makeLoadProjectsRequest';

export default class ProjectDataService extends BaseDataService {
  public async createProject(
    settings: IProjectCreationSettings
  ): Promise<Project> {
    const request = (() => {
      const requestFields: {
        [K in keyof Required<IProjectCreationSettings>]: [string, any]
      } = {
        name: ['name', settings.name],
        visibility: [
          'project_visibility',
          convertProjectVisibilityToServer(settings.visibility),
        ],
        description: ['description', settings.description],
        tags: ['tags', settings.tags],
      };
      return R.fromPairs(R.values(requestFields));
    })();

    const response = await this.post<any, typeof projectAlreadyExistsError>({
      url: `/v1/modeldb/project/createProject`,
      data: request,
      errorConverters: {
        projectAlreadyExists: errorResponse => errorResponse.status === 409,
      },
    });
    return convertHydratedProjectToClient({ project: response.data.project });
  }

  public async loadProject(projectId: string): Promise<Project> {
    const response = await this.get<any, EntityErrorType>({
      url: '/v1/modeldb/hydratedData/getHydratedProjectById',
      config: { params: { id: projectId } },
      errorConverters: {
        accessDeniedToEntity: error => error.status === 403,
        entityNotFound: error => error.status === 404,
      },
    });
    return convertHydratedProjectToClient(response.data.hydrated_project);
  }

  public async loadProjects(
    filters: IFilterData[],
    pagination: IPagination | undefined,
    workspaceName: IWorkspace['name']
  ): Promise<DataWithPagination<Project>> {
    const request = await makeLoadProjectsRequest(
      filters,
      pagination,
      workspaceName
    );
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedProjects',
      data: request,
    });

    if (!response.data || !response.data.hydrated_projects) {
      return { data: [], totalCount: 0 };
    }
    const projects: Project[] = response.data.hydrated_projects.map(
      convertHydratedProjectToClient
    );

    return {
      data: projects,
      totalCount: Number(response.data.total_records),
    };
  }

  public async deleteProject(id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/project/deleteProject',
      config: { data: { id } },
    });
  }

  public async deleteProjects(ids: string[]): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/project/deleteProjects',
      config: { data: { ids } },
    });
  }

  public async updateReadme(id: string, readme: Markdown): Promise<void> {
    await this.post({
      url: '/v1/modeldb/project/setProjectReadme',
      data: { id, readme_text: readme },
    });
  }

  public async loadArtifactUrl(
    projectId: string,
    artifact: IArtifact
  ): Promise<string> {
    const response = await this.post({
      url: '/v1/modeldb/project/getUrlForArtifact',
      data: {
        id: projectId,
        key: artifact.key,
        artifact_type: artifact.type,
        method: 'GET',
      },
    });
    return response.data.url;
  }
}
