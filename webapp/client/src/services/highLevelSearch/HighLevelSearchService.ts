import { bind } from 'decko';
import * as R from 'ramda';
import { ApolloClient, gql } from 'apollo-boost';
import * as graphqlGlobalTypes from 'graphql-types/graphql-global-types';

import {
  ISearchSettings,
  Entities,
  ExperimentResult,
  ExperimentRunResult,
  ProjectResult,
  DatasetResult,
  EntitiesBySearchFields,
  IResult,
  getEntitiesBySearchFields,
  filterMapEntitiesBySearchFields,
  RepositoryResult,
} from 'core/shared/models/HighLevelSearch';
import { BaseDataService } from 'services/BaseDataService';
import {
  IPaginationSettings,
  DataWithPagination,
  IPagination,
} from 'core/shared/models/Pagination';
import { ProjectDataService } from 'services/projects';
import { IWorkspace } from 'core/shared/models/Workspace';
import {
  makeDefaultTagFilter,
  IStringFilterData,
  makeDefaultNameFilter,
} from 'core/features/filter/Model';
import { ExperimentRunsDataService } from 'services/experimentRuns';
import { ExperimentsDataService } from 'services/experiments';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { RecordFromUnion, RecordValues } from 'core/shared/utils/types';
import { DatasetsDataService } from 'services/datasets';
import { ISorting } from 'core/shared/models/Sorting';
import matchType from 'core/shared/utils/matchType';
import { getServerFilterOperator } from 'core/features/filter/service/serverModel/Filters/Filters';

import * as graphqlTypes from './graphql-types/RepositoriesResult';
import { paginationSettings } from 'core/features/highLevelSearch/constants';

export type ILoadEntitiesByTypeResult = RecordValues<
  RecordFromUnion<
    Entities,
    {
      projects: {
        type: 'projects';
        data: EntitiesBySearchFields<ProjectResult>;
      };
      experiments: {
        type: 'experiments';
        data: EntitiesBySearchFields<ExperimentResult>;
      };
      experimentRuns: {
        type: 'experimentRuns';
        data: EntitiesBySearchFields<ExperimentRunResult>;
      };
      datasets: {
        type: 'datasets';
        data: EntitiesBySearchFields<DatasetResult>;
      };
      repositories: {
        type: 'repositories';
        data: EntitiesBySearchFields<RepositoryResult>;
      };
    }
  >
>;

export default class HighLevelSearchService extends BaseDataService {
  private apolloClient: ApolloClient<any>;

  constructor(apolloClient: ApolloClient<any>) {
    super();
    this.apolloClient = apolloClient;
  }

  @bind
  public async loadFullEntitiesByType(
    settings: ILoadEntitiesSettings
  ): Promise<ILoadEntitiesByTypeResult> {
    const type = settings.searchSettings.type;
    switch (type) {
      case 'projects': {
        const data = await this.loadProjectsBySearchFields(settings);
        const res: ILoadEntitiesByTypeResult = {
          type,
          data,
        };
        return res;
      }
      case 'experimentRuns': {
        const experimentRunsBySearchFields = await this.loadExperimentRunsBySearchFields(
          settings
        );
        const projectsOfRunsPromise = new ProjectDataService().loadShortProjectsByIds(
          settings.workspaceName,
          R.uniq(
            getEntitiesBySearchFields(experimentRunsBySearchFields).map(
              ({ projectId }) => projectId
            )
          )
        );
        const experimentsOfRunsPromise = new ExperimentsDataService().loadExperimentsByIdsAndWorkspace(
          settings.workspaceName,
          R.uniq(
            getEntitiesBySearchFields(experimentRunsBySearchFields).map(
              ({ experimentId }) => experimentId
            )
          )
        );
        const [projectsOfRuns, experimentsOfRuns] = await Promise.all([
          projectsOfRunsPromise,
          experimentsOfRunsPromise,
        ]);
        const experimentRunsResults = filterMapEntitiesBySearchFields(
          expRun => {
            const project = projectsOfRuns.find(p => p.id === expRun.projectId);
            const experiment = experimentsOfRuns.find(
              e => e.id === expRun.experimentId
            );
            if (project && experiment) {
              const res: ExperimentRunResult = {
                entityType: 'experimentRun',
                ...expRun,
                experiment,
                project,
              };
              return res;
            }
            return undefined;
          },
          experimentRunsBySearchFields
        );
        const res: ILoadEntitiesByTypeResult = {
          type,
          data: experimentRunsResults,
        };
        return res;
      }
      case 'experiments': {
        const experimentsBySearchFields = await this.loadExperimentsBySeachFields(
          settings
        );
        const projectsOfRuns = await new ProjectDataService().loadShortProjectsByIds(
          settings.workspaceName,
          R.uniq(
            getEntitiesBySearchFields(experimentsBySearchFields).map(
              ({ projectId }) => projectId
            )
          )
        );
        const experimentResults = filterMapEntitiesBySearchFields(
          experiment => {
            const project = projectsOfRuns.find(
              p => p.id === experiment.projectId
            );
            if (project) {
              const res: ExperimentResult = {
                entityType: 'experiment',
                ...experiment,
                project,
              };
              return res;
            }
            return undefined;
          },
          experimentsBySearchFields
        );
        const res: ILoadEntitiesByTypeResult = {
          type,
          data: experimentResults,
        };
        return res;
      }
      case 'datasets': {
        const datasets = await this.loadDatasetsBySearchFields(settings);
        const res: ILoadEntitiesByTypeResult = {
          type,
          data: datasets,
        };
        return res;
      }
      case 'repositories': {
        const repositories = await this.loadRepositoriesBySearchFields(
          settings
        );
        const res: ILoadEntitiesByTypeResult = {
          type: 'repositories',
          data: repositories,
        };
        return res;
      }
      default:
        return exhaustiveCheck(type, '');
    }
  }

  @bind
  public async loadFullEntitiesByTypeAndUpdateOthersCounts(
    handlers: {
      onSuccess: (res: ILoadEntitiesByTypeResult) => void;
      onError: (error: Error) => void;
    },
    settings: ILoadEntitiesSettings,
    isEnableRepositories: boolean
  ) {
    const entitiesCountLoaders: {
      [K in Entities]: (
        settings: ILoadEntitiesSettings
      ) => Promise<{ totalCount: number }>
    } = {
      projects: this.loadProjectsBySearchFields,
      experiments: this.loadExperimentsBySeachFields,
      datasets: this.loadDatasetsBySearchFields,
      repositories: this.loadRepositoriesBySearchFields,
      experimentRuns: this.loadExperimentRunsBySearchFields,
    };

    const promises = moveFirstItem(
      ([entityType]) => settings.searchSettings.type === entityType,
      0,
      Object.entries(entitiesCountLoaders)
    )
      .filter(([entityType, loadEntitiesCount]) =>
        isEnableRepositories ? true : entityType !== 'repositories'
      )
      .map(([entityType, loadEntitiesCount]) => {
        return entityType === settings.searchSettings.type
          ? this.loadFullEntitiesByType(settings)
              .then(({ type, data }) =>
                handlers.onSuccess({ type, data } as ILoadEntitiesByTypeResult)
              )
              .catch(handlers.onError)
          : loadEntitiesCount(settings)
              .then(data =>
                handlers.onSuccess({
                  type: entityType,
                  data,
                } as ILoadEntitiesByTypeResult)
              )
              .catch(handlers.onError);
      });

    await Promise.all(promises);
  }

  private loadProjectsBySearchFields = this.makeLoadEntitiesBySearchFields(
    'project',
    (filters, pagination, workspaceName, sorting) =>
      new ProjectDataService().loadProjects(
        filters,
        pagination,
        workspaceName,
        sorting
      )
  );

  private loadExperimentsBySeachFields = this.makeLoadEntitiesBySearchFields(
    'experiment',
    (filters, pagination, workspaceName, sorting) =>
      new ExperimentsDataService().loadExperimentsByWorkspace(
        filters,
        pagination,
        workspaceName,
        sorting
      )
  );

  private loadExperimentRunsBySearchFields = this.makeLoadEntitiesBySearchFields(
    'experimentRun',
    (filters, pagination, workspaceName, sorting) =>
      new ExperimentRunsDataService().loadExperimentRunsByWorkspace(
        workspaceName,
        filters,
        pagination,
        sorting,
        false
      )
  );

  private loadDatasetsBySearchFields = this.makeLoadEntitiesBySearchFields(
    'dataset',
    (filters, pagination, workspaceName) =>
      new DatasetsDataService().loadDatasets(filters, pagination, workspaceName)
  );

  private loadRepositoriesBySearchFields = this.makeLoadEntitiesBySearchFields(
    'repository',
    async (filters, pagination, workspaceName) => {
      const convertFilters = (
        filters: IStringFilterData[]
      ): graphqlGlobalTypes.StringPredicate[] => {
        return filters.map(filter => {
          const res: graphqlGlobalTypes.StringPredicate = {
            key: filter.name === 'tags' ? 'labels' : filter.name,
            operator: (getServerFilterOperator(
              filter
            ) as any) as graphqlGlobalTypes.StringPredicate['operator'],
            value: filter.value,
          };
          return res;
        });
      };

      const response = await this.apolloClient.query<
        graphqlTypes.RepositoriesResult,
        graphqlTypes.RepositoriesResultVariables
      >({
        query: REPOSITORIES_RESULT,
        fetchPolicy: 'network-only',
        variables: {
          workspaceName,
          filters: convertFilters(filters),
          pagination: {
            limit: pagination.pageSize,
            page: pagination.currentPage,
          },
        },
      });
      if (!response.data.workspace || (response.errors || []).length > 0) {
        return { totalCount: 0, data: [] };
      }
      return {
        data: response.data.workspace.repositories.repositories.map(
          repository => ({
            ...repository,
            entityType: 'repository' as const,
            dateCreated: new Date(Number(repository.dateCreated)),
            dateUpdated: new Date(Number(repository.dateUpdated)),
          })
        ),
        totalCount:
          response.data.workspace.repositories.pagination.totalRecords,
      };
    }
  );

  private makeLoadEntitiesBySearchFields<
    E extends IResult['entityType'],
    T extends { id: string }
  >(
    entityType: E,
    f: (
      filters: IStringFilterData[],
      pagination: IPagination,
      workspaceName: IWorkspace['name'],
      sorting: ISorting | undefined
    ) => Promise<DataWithPagination<T>>
  ) {
    return async (
      settings: ILoadEntitiesSettings
    ): Promise<Required<EntitiesBySearchFields<T & { entityType: E }>>> => {
      const entitiesByTypes = await this.loadEntitiesBySearchFields(
        f,
        settings
      );
      return this.getDisplayedEntitiesBySearchFields({
        entityType,
        ...entitiesByTypes,
      });
    };
  }

  private async loadEntitiesBySearchFields<T extends { id: string }>(
    f: (
      filters: IStringFilterData[],
      pagination: IPagination,
      workspaceName: IWorkspace['name'],
      sorting: ISorting | undefined
    ) => Promise<DataWithPagination<T>>,
    settings: ILoadEntitiesSettings
  ) {
    const pagination = { ...settings.pagination, totalCount: 0 };
    const sorting = settings.searchSettings.sorting
      ? {
          direction: settings.searchSettings.sorting.direction,
          columnName: undefined,
          fieldName: matchType(
            {
              dateCreated: () => 'date_created',
              dateUpdated: () => 'date_updated',
            },
            settings.searchSettings.sorting.field
          ),
        }
      : undefined;

    const entitiesByNamesPromise = f(
      settings.searchSettings.nameOrTag
        ? [makeDefaultNameFilter(settings.searchSettings.nameOrTag, 'LIKE')]
        : [],
      pagination,
      settings.workspaceName,
      sorting
    ).catch(() => ({ totalCount: 0, data: [] as T[] }));
    const entitiesByTagsPromise = settings.searchSettings.nameOrTag
      ? f(
          settings.searchSettings.nameOrTag
            ? [makeDefaultTagFilter(settings.searchSettings.nameOrTag, 'LIKE')]
            : [],
          pagination,
          settings.workspaceName,
          sorting
        ).catch(() => ({ totalCount: 0, data: [] as T[] }))
      : Promise.resolve({ totalCount: 0, data: [] as T[] });

    const [entitiesByNames, entitiesByTags] = await Promise.all([
      entitiesByNamesPromise,
      entitiesByTagsPromise,
    ]);
    return {
      entitiesByNames,
      entitiesByTags,
    };
  }

  private getDisplayedEntitiesBySearchFields<
    T extends { id: string },
    E extends IResult['entityType']
  >({
    entityType,
    entitiesByNames,
    entitiesByTags,
  }: {
    entityType: E;
    entitiesByNames: DataWithPagination<T>;
    entitiesByTags: DataWithPagination<T>;
  }) {
    if (
      Math.max(entitiesByNames.totalCount, entitiesByTags.totalCount) <=
      paginationSettings.pageSize
    ) {
      const entitiesByNameWithoutEntityByTag = entitiesByNames.data
        .filter(x =>
          entitiesByTags.data.every(entityByTag => entityByTag.id !== x.id)
        )
        .map(x => ({ ...x, entityType }));
      const res = {
        totalCount:
          entitiesByNameWithoutEntityByTag.length + entitiesByTags.totalCount,
        data: {
          tag: entitiesByTags.data.map(x => ({ ...x, entityType })),
          name: entitiesByNameWithoutEntityByTag,
        },
      };
      return res;
    }
    const res = {
      totalCount: entitiesByNames.totalCount + entitiesByTags.totalCount,
      data: {
        name: entitiesByNames.data.map(x => ({ ...x, entityType })),
        tag: entitiesByTags.data.map(x => ({ ...x, entityType })),
      },
    };
    return res;
  }
}

const REPOSITORIES_RESULT = gql`
  query RepositoriesResult(
    $workspaceName: String!
    $filters: [StringPredicate!]!
    $pagination: PaginationQuery!
  ) {
    workspace(name: $workspaceName) {
      name
      repositories(
        query: { stringPredicates: $filters, pagination: $pagination }
      ) {
        repositories {
          id
          name
          owner {
            username
          }
          dateCreated
          dateUpdated
          labels
        }
        pagination {
          totalRecords
        }
      }
    }
  }
`;

export interface ILoadEntitiesSettings {
  workspaceName: IWorkspace['name'];
  pagination: IPaginationSettings;
  searchSettings: ISearchSettings;
}

const moveFirstItem = <T>(
  pred: (item: T) => Boolean,
  to: number,
  items: T[]
): T[] => {
  const targetItemIndex = items.findIndex(pred);
  return R.move(targetItemIndex, to, items);
};
