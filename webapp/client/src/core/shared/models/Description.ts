import { validateMaxLength } from 'core/shared/utils/validators';

import * as Common from './Common';

export type EntityWithDescription = Common.EntityType;

export type Description = string;
export const validateDescription = validateMaxLength(256);
