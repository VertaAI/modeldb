import { validateMaxLength } from 'shared/utils/validators';

import * as Common from './Common';

export type EntityWithDescription = Extract<
  Common.EntityType,
  'project' | 'experiment' | 'experimentRun' | 'dataset' | 'datasetVersion'
>;

export type Description = string;
export const validateDescription = validateMaxLength(256);
