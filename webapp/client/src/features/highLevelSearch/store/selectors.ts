import { IApplicationState } from 'store/store';

const selectState = (state: IApplicationState) => state.highLevelSearch;

export const selectEntitiesResults = (state: IApplicationState) => {
  return selectState(state).data.entitiesResults;
};

export const selectRedirectTo = (state: IApplicationState) => {
  return selectState(state).data.redirectTo;
};
