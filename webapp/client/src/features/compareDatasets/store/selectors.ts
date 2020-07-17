import { IApplicationState } from 'setup/store/store';

import {
  ICompareDatasetsState, ComparedDatasetVersionIds,
} from './types';
import { ComparedDatasetVersions, getDatasetVersionsDifferentProps } from './compareDatasets';
import { selectDatasetVersion } from 'features/datasetVersions';

const selectState = (state: IApplicationState): ICompareDatasetsState =>
  state.compareDatasets;

export const selectComparedEntityIds = (
  state: IApplicationState,
  projectId: string
) => selectState(state).data.comparedEntityIdsByContainerId[projectId] || [];

export const selectIsEnableEntitiesComparing = (
  state: IApplicationState,
  projectId: string
) => selectComparedEntityIds(state, projectId).length >= 2;

export const selectIsDisabledSelectionEntitiesForComparing = (
  state: IApplicationState,
  projectId: string
) => false;

export const selectIsComparedEntity = (
  state: IApplicationState,
  projectId: string,
  modelId: string
) => selectComparedEntityIds(state, projectId).includes(modelId);

export const selectComparedDatasetVersions = (
  state: IApplicationState,
  datasetVersionIds: Required<ComparedDatasetVersionIds>
): ComparedDatasetVersions => {
  return datasetVersionIds
      .map(id => selectDatasetVersion(state, id!))
      .filter(Boolean) as ComparedDatasetVersions;
};

export const selectDatasetVersionsDifferentProps = (
  state: IApplicationState,
  datasetVersionIds: Required<ComparedDatasetVersionIds>
) => {
  const comparedDatasetVersions: ComparedDatasetVersions = selectComparedDatasetVersions(
      state,
      datasetVersionIds
  );
  return comparedDatasetVersions.length === 2
      ? getDatasetVersionsDifferentProps(
          ...(comparedDatasetVersions as Required<ComparedDatasetVersions>)
      )
      : undefined;
};
