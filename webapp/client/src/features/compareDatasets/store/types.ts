export interface ICompareDatasetsState {
  data: {
    comparedEntityIdsByContainerId: Record<
      string,
      ComparedDatasetVersionIds | undefined
    >;
  };
}

export type ComparedDatasetVersionIds = [string?, string?];

export enum selectEntityForComparingActionType {
  SELECT_ENTITY_FOR_COMPARING = '@@compareEntities/SELECT_ENTITY_FOR_COMPARING',
}
export interface ISelectEntityForComparing {
  type: selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: string };
}
export enum unselectEntityForComparingActionType {
  UNSELECT_ENTITY_FOR_COMPARING = '@@compareEntities/UNSELECT_ENTITY_FOR_COMPARING',
}
export interface IUnselectEntityForComparing {
  type: unselectEntityForComparingActionType.UNSELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: string };
}

export type FeatureAction =
  | ISelectEntityForComparing
  | IUnselectEntityForComparing;
