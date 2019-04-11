import { IApplicationState } from '../store';
import { IModelRecordState } from './types';

const selectState = (state: IApplicationState): IModelRecordState =>
  state.modelRecord;

export const selectModelRecord = (state: IApplicationState) =>
  selectState(state).data.modelRecord;

export const selectIsLoadingModelRecord = (state: IApplicationState) =>
  selectCommunications(state).loadingModelRecord.isRequesting;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;
