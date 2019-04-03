import { IApplicationState } from '../store';
import { IModelRecordState } from './types';

const selectState = (state: IApplicationState): IModelRecordState =>
  state.modelRecord;

export const selectModelRecord = (state: IApplicationState) =>
  selectState(state).data;

export const selectIsLoadingModelRecord = (state: IApplicationState) =>
  selectState(state).loading;
