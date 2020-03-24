import { bind } from 'decko';
import * as R from 'ramda';

import { BaseDataService } from 'core/services/BaseDataService';
import { HttpError } from 'core/shared/models/Error';
import {
  DataWithPagination,
  IPaginationSettings,
} from 'core/shared/models/Pagination';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  ICommit,
  IHydratedCommit,
  CommitTag,
  IDataRequest,
  Branch,
  defaultBranch,
  CommitPointer,
  IRepositoryData,
  emptyFolder,
  ICommitWithData,
} from 'core/shared/models/Versioning/RepositoryData';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { convertClientPaginationToNamespacedServerPagination } from 'core/services/serverModel/Pagination/converters';
import UsersService from 'core/services/users/UsersService';

import { convertServerBlobToClient } from '../../serverModel/Versioning/RepositoryData/Blob';
import {
  convertServerFolderToClient,
  convertServerCommitToClient,
} from '../../serverModel/Versioning/RepositoryData/converters';
import { unknownUser } from 'models/User';
import ModelRecord, { IExperimentRunInfo } from 'models/ModelRecord';
import { ExperimentsDataService } from 'services/experiments';
import { ProjectDataService } from 'services/projects';
import { IWorkspace } from 'models/Workspace';
import { mapObj } from 'core/shared/utils/collection';
import { JsonConvert } from 'json2typescript';

export default class RepositoryDataService extends BaseDataService {
  constructor() {
    super();
  }

  @bind
  public async loadCommitWithData({
    repositoryId,
    fullDataLocationComponents: settings,
  }: IDataRequest): Promise<ICommitWithData> {
    const commit = await this.loadCommitByPointer(
      repositoryId,
      settings.commitPointer
    );

    const data = await this.loadCommitData({
      repositoryId,
      commitSha: commit.sha,
      location: settings.location,
    });

    return { commit, data };
  }

  @bind
  public async loadTagCommit({
    repositoryId,
    tag,
  }: {
    repositoryId: IRepository['id'];
    tag: CommitTag;
  }) {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/tags/${tag}`,
    });
    const result = convertServerCommitToClient(response.data.commit);
    const author = await new UsersService().loadUser(result.authorId);
    return { ...result, author };
  }

  @bind
  public async loadCommitData({
    commitSha,
    repositoryId,
    location,
  }: {
    repositoryId: IRepository['id'];
    commitSha: ICommit['sha'];
    location: DataLocation.DataLocation;
  }): Promise<IRepositoryData> {
    const { data } = await this.get({
      url: DataLocation.addAsLocationQueryParams(
        location,
        `/v1/modeldb/versioning/repositories/${repositoryId}/commits/${commitSha}/path`
      ),
    });

    if (data.blob) {
      return convertServerBlobToClient(data.blob);
    }

    if (data.folder) {
      return convertServerFolderToClient(data.folder);
    }

    return emptyFolder;
  }

  @bind
  public async loadTags({
    repositoryId,
  }: {
    repositoryId: IRepository['id'];
  }): Promise<CommitTag[]> {
    const response = await this.get<{
      tags?: CommitTag[];
      total_records?: number;
    }>({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/tags`,
    });

    return response.data.tags || [];
  }

  @bind
  public async loadBranches(
    repositoryId: IRepository['id']
  ): Promise<Branch[]> {
    const response = await this.get<{
      branches?: Branch[];
      total_records?: number;
    }>({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/branches`,
    });

    return R.uniq((response.data.branches || []).concat(defaultBranch));
  }

  @bind
  public async loadBranchCommits({
    repositoryId,
    branch,
    paginationSettings,
    location,
  }: {
    repositoryId: IRepository['id'];
    branch: Branch;
    paginationSettings: IPaginationSettings;
    location: DataLocation.DataLocation;
  }): Promise<DataWithPagination<IHydratedCommit>> {
    const response = await this.get({
      url: DataLocation.addAsLocationPrefixQueryParams(
        location,
        `/v1/modeldb/versioning/repositories/${repositoryId}/branches/${branch}/log`
      ),
      config: {
        params: {
          ...convertClientPaginationToNamespacedServerPagination(
            paginationSettings
          ),
        },
      },
    });

    if (!response.data || !response.data.commits) {
      return {
        data: [],
        totalCount: 0,
      };
    }

    const users = await new UsersService().loadUsers(
      response.data.commits.map(({ author }: any) => author)
    );

    return {
      data: response.data.commits
        .map(convertServerCommitToClient)
        .map((c: ICommit) => ({
          ...c,
          author: users.find(user => user.id === c.authorId) || unknownUser,
        })),
      totalCount: Number(response.data.total_records),
    };
  }

  @bind
  public async loadCommit({
    repositoryId,
    commitSha,
  }: {
    repositoryId: IRepository['id'];
    commitSha: ICommit['sha'];
  }): Promise<IHydratedCommit> {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/commits/${commitSha}`,
    });
    const author = await new UsersService().loadUser(
      response.data.commit.author
    );
    return { ...convertServerCommitToClient(response.data.commit), author };
  }

  @bind
  public async loadCommitByPointer(
    repositoryId: IRepository['id'],
    commitPointer: CommitPointer
  ): Promise<IHydratedCommit> {
    switch (commitPointer.type) {
      case 'branch': {
        return this.loadLastBranchCommit(repositoryId, commitPointer.value);
      }
      case 'tag': {
        return this.loadTagCommit({
          repositoryId,
          tag: commitPointer.value,
        });
      }
      case 'commitSha': {
        return this.loadCommit({
          repositoryId,
          commitSha: commitPointer.value,
        });
      }
      default:
        return exhaustiveCheck(commitPointer, '');
    }
  }

  @bind
  public async loadCommitExperimentRuns({
    repositoryId,
    commitSha,
    workspaceName,
  }: {
    repositoryId: IRepository['id'];
    commitSha: ICommit['sha'];
    workspaceName: IWorkspace['name'];
  }): Promise<IExperimentRunInfo[]> {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/commits/${commitSha}/runs`,
    });
    if (!response.data || !response.data.runs) {
      return [];
    }
    return await this.loadProjectsAndExperimentsForExperimentRuns(
      workspaceName,
      response.data.runs
    );
  }

  @bind
  public async loadBlobExperimentRuns({
    repositoryId,
    commitSha,
    location,
    workspaceName,
  }: {
    repositoryId: IRepository['id'];
    commitSha: ICommit['sha'];
    location: DataLocation.DataLocation;
    workspaceName: IWorkspace['name'];
  }): Promise<IExperimentRunInfo[]> {
    const response = await this.get({
      url: DataLocation.addAsLocationQueryParams(
        location,
        `/v1/modeldb/versioning/repositories/${repositoryId}/commits/${commitSha}/path/runs`
      ),
    });
    if (!response.data || !response.data.runs) {
      return [];
    }
    return await this.loadProjectsAndExperimentsForExperimentRuns(
      workspaceName,
      response.data.runs
    );
  }

  @bind
  private async loadLastBranchCommit(
    repositoryId: IRepository['id'],
    branch: Branch
  ): Promise<IHydratedCommit> {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/branches/${branch}/log`,
      config: {
        params: convertClientPaginationToNamespacedServerPagination({
          currentPage: 0,
          pageSize: 1,
        }),
      },
    });
    const usersService = new UsersService();

    if (response.data.commits && response.data.commits[0]) {
      const result = convertServerCommitToClient(response.data.commits[0]);
      const author = await usersService.loadUser(result.authorId);
      return { ...result, author };
    }

    throw new HttpError({ status: 500 });
  }

  @bind
  private async loadProjectsAndExperimentsForExperimentRuns(
    workspaceName: IWorkspace['name'],
    serverExperimentRuns: any[]
  ): Promise<IExperimentRunInfo[]> {
    if (serverExperimentRuns.length === 0) {
      return [];
    }
    const experimentRuns = (() => {
      const jsonConvert = new JsonConvert();
      const res = jsonConvert.deserializeArray(
        serverExperimentRuns,
        ModelRecord
      );
      return res;
    })();

    const projectsPromise = new ProjectDataService().loadShortProjectsByIds(
      workspaceName,
      R.uniq(experimentRuns.map(({ projectId }) => projectId))
    );

    const experimentsPromise = (() => {
      const experimentIdssByProjectId = R.pipe(
        R.map<ModelRecord, { projectId: string; experimentId: string }>(
          ({ projectId, experimentId }) => ({ projectId, experimentId })
        ),
        R.groupBy(({ projectId }) => projectId),
        x =>
          mapObj(
            items => R.uniq(items.map(({ experimentId }) => experimentId)),
            x
          )
      )(experimentRuns);

      return Promise.all(
        R.toPairs(experimentIdssByProjectId).map(([projectId, experimentIds]) =>
          new ExperimentsDataService().loadExperimentsByIds(
            projectId,
            experimentIds
          )
        )
      ).then(experiments => experiments.flatMap(x => x));
    })();
    const [projects, experiments] = await Promise.all([
      projectsPromise,
      experimentsPromise,
    ]);

    return experimentRuns
      .map(experimentRun => {
        const project = projects.find(p => p.id === experimentRun.projectId);
        const experiment = experiments.find(
          e => e.id === experimentRun.experimentId
        );
        return project && experiment
          ? {
              experimentRun: {
                ...experimentRun,
                shortExperiment: experiment,
              },
              project,
            }
          : undefined;
      })
      .filter(Boolean) as IExperimentRunInfo[];
  }
}
