import ModelRecord from 'shared/models/ModelRecord';

export interface ICompareModelsState {
  data: {
    comparedEntityIdsByContainerId: Record<
      string,
      ComparedModelIds | undefined
    >;
  };
}

export type ComparedModelIds = string[];

export enum selectEntityForComparingActionType {
  SELECT_ENTITY_FOR_COMPARING = '@@compareEntities/SELECT_ENTITY_FOR_COMPARING',
}
export interface ISelectEntityForComparing {
  type: selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: ModelRecord['id'] };
}
export enum unselectEntityForComparingActionType {
  UNSELECT_ENTITY_FOR_COMPARING = '@@compareEntities/UNSELECT_ENTITY_FOR_COMPARING',
}
export interface IUnselectEntityForComparing {
  type: unselectEntityForComparingActionType.UNSELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: ModelRecord['id'] };
}

export type FeatureAction =
  | ISelectEntityForComparing
  | IUnselectEntityForComparing;
