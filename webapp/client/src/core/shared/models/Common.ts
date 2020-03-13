export interface IKeyValuePair<T> {
  key: string;
  value: T;
}

// todo to find better name
export interface IEntityWithLogging {
  dateCreated: Date;
  dateUpdated: Date;
}

export const accessDeniedToEntityError = 'accessDeniedToEntity';
export const entityNotFoundError = 'entityNotFound';

export type EntityErrorType =
  | typeof accessDeniedToEntityError
  | typeof entityNotFoundError;

export type EntityType =
  | 'project'
  | 'experiment'
  | 'experimentRun'
  | 'dataset'
  | 'datasetVersion';
export const EntityType: { [K in EntityType]: K } = {
  dataset: 'dataset',
  datasetVersion: 'datasetVersion',
  experiment: 'experiment',
  experimentRun: 'experimentRun',
  project: 'project',
};
