import * as Common from './Common';

export type EntityWithTags = Extract<
  Common.EntityType,
  'project' | 'experiment' | 'experimentRun' | 'dataset' | 'datasetVersion'
>;
