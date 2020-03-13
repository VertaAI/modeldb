import { IApplicationState } from '../store';
import { IArtifactManagerState } from './types';

const selectState = (state: IApplicationState): IArtifactManagerState =>
  state.artifactManager;

export const selectArtifactUrl = (state: IApplicationState) =>
  selectState(state).data.url;

export const selectArtifactPreview = (state: IApplicationState) =>
  selectState(state).data.preview;

export const selectDownloadingArtifact = (state: IApplicationState) =>
  selectCommunications(state).downloadingArtifact;

export const selectLoadingArtifactPreview = (state: IApplicationState) =>
  selectCommunications(state).loadingArtifactPreview;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectDatasetVersion = (state: IApplicationState, id: string) =>
  selectState(state).data.datasetVersions[id];

export const selectDatasetVersions = (state: IApplicationState) =>
  selectState(state).data.datasetVersions;
