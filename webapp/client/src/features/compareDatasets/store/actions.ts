import {
    ISelectEntityForComparing,
    IUnselectEntityForComparing,
    selectEntityForComparingActionType,
    unselectEntityForComparingActionType,
  } from './types';
  
  export const selectEntityForComparing = (
    payload: ISelectEntityForComparing['payload']
  ): ISelectEntityForComparing => ({
    type: selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING,
    payload,
  });
  export const unselectEntityForComparing = (
    payload: IUnselectEntityForComparing['payload']
  ): IUnselectEntityForComparing => ({
    type: unselectEntityForComparingActionType.UNSELECT_ENTITY_FOR_COMPARING,
    payload,
  });
  