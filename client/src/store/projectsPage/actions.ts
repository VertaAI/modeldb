import { hideDeveloperKeyInfoActionType, IHideDeveloperKeyInfo } from './types';

export const hideDeveloperKeyInfo = (): IHideDeveloperKeyInfo => ({
  type: hideDeveloperKeyInfoActionType,
});
