import { RecordFromUnion } from 'shared/utils/types';
import Experiment from 'shared/models/Experiment';
import { Project } from 'shared/models/Project';
import { ICommunication } from 'shared/utils/redux/communication';

import { ISorting } from '../Sorting';
import { IRepository } from '../Versioning/Repository';

export type Entities =
  | 'projects'
  | 'experiments'
  | 'experimentRuns'
  | 'datasets'
  | 'repositories';
export const Entities: { [T in Entities]: T } = {
  projects: 'projects',
  experiments: 'experiments',
  experimentRuns: 'experimentRuns',
  datasets: 'datasets',
  repositories: 'repositories',
};

export interface IResultsSorting {
  field: keyof Pick<EntityResultCommonData, 'dateCreated' | 'dateUpdated'>;
  direction: ISorting['direction'];
}
export const defaultResultsSorting: IResultsSorting = {
  direction: 'desc',
  field: 'dateUpdated',
};

export type ActiveFilter = Entities;

export type SearchField = 'tag' | 'name';

export type EntitiesBySearchFields<T> = {
  data?: Record<SearchField, T[]>;
  totalCount: number;
};
export const filterMapEntitiesBySearchFields = <
  T extends EntitiesBySearchFields<any>,
  R
>(
  f: (
    entitiesBySearchFields: T extends EntitiesBySearchFields<infer Z>
      ? Z
      : never
  ) => R,
  entitiesBySearchFields: T
): EntitiesBySearchFields<Exclude<R, null | undefined>> => {
  return {
    ...entitiesBySearchFields,
    data: {
      name: (entitiesBySearchFields.data || { name: [], tag: [] }).name
        .map((x) => f(x))
        .filter((x): x is Exclude<R, null | undefined> => Boolean(x)),
      tag: (entitiesBySearchFields.data || { name: [], tag: [] }).tag
        .map((x) => f(x))
        .filter((x): x is Exclude<R, null | undefined> => Boolean(x)),
    },
  };
};
export const getEntitiesBySearchFields = <
  T extends EntitiesBySearchFields<any>
>(
  entitiesBySearchFields: T
): T extends EntitiesBySearchFields<infer Z> ? Z[] : never => {
  return entitiesBySearchFields.data
    ? (entitiesBySearchFields.data.name.concat(
        entitiesBySearchFields.data.tag
      ) as any)
    : [];
};

export type IEntitiesResults = RecordFromUnion<
  Entities,
  {
    projects: {
      communication: ICommunication;
      data: EntitiesBySearchFields<ProjectResult>;
    };
    experiments: {
      communication: ICommunication;
      data: EntitiesBySearchFields<ExperimentResult>;
    };
    experimentRuns: {
      communication: ICommunication;
      data: EntitiesBySearchFields<ExperimentRunResult>;
    };
    datasets: {
      communication: ICommunication;
      data: EntitiesBySearchFields<DatasetResult>;
    };
    repositories: {
      communication: ICommunication;
      data: EntitiesBySearchFields<RepositoryResult>;
    };
  }
>;

export type EntityResultCommonData = Pick<
  Project,
  'name' | 'id' | 'dateCreated' | 'tags' | 'dateUpdated'
>;

export type ExperimentResult = EntityResultCommonData & {
  project: Pick<Project, 'name' | 'id'>;
  entityType: 'experiment';
};

export type ExperimentRunResult = EntityResultCommonData & {
  project: Pick<Project, 'name' | 'id'>;
  experiment: Pick<Experiment, 'name' | 'id'>;
  entityType: 'experimentRun';
};

export type ProjectResult = EntityResultCommonData & { entityType: 'project' };

export type DatasetResult = EntityResultCommonData & { entityType: 'dataset' };

export type RepositoryResult = Omit<EntityResultCommonData, 'tags'> & {
  labels: IRepository['labels'];
  entityType: 'repository';
};

export type IResult =
  | ProjectResult
  | ExperimentRunResult
  | ExperimentResult
  | DatasetResult
  | RepositoryResult;

export interface ISearchSettings {
  nameOrTag: string;
  type: ActiveFilter;
  currentPage: number;
  sorting: IResultsSorting | undefined;
}

export const changeSorting = (
  sorting: IResultsSorting | undefined,
  searchSettings: ISearchSettings
) => {
  return resetPaginationPage({
    ...searchSettings,
    sorting,
  });
};

export const changeFilter = (
  type: ActiveFilter,
  searchSettings: ISearchSettings
): ISearchSettings => {
  return resetPaginationPage({
    ...searchSettings,
    type,
  });
};

export const changePaginationPage = (
  page: number,
  searchSettings: ISearchSettings
): ISearchSettings => {
  return {
    ...searchSettings,
    currentPage: page,
  };
};

export const changeNameOrTag = (
  nameOrTag: string,
  searchSettings: ISearchSettings
): ISearchSettings => {
  return resetPaginationPage({
    ...searchSettings,
    nameOrTag: nameOrTag,
  });
};

const resetPaginationPage = (
  searchSettings: ISearchSettings
): ISearchSettings => {
  return {
    ...searchSettings,
    currentPage: 0,
  };
};
