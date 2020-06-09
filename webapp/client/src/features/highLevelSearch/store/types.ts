import { Location } from 'history';

import { IEntitiesResults } from '../../../core/shared/models/HighLevelSearch';

export interface IHighLevelSearchState {
  data: {
    entitiesResults: IEntitiesResults;
    redirectTo: Location | null;
  };
}
