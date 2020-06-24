import * as Common from 'shared/models/Common';
import { AppError } from 'shared/models/Error';
import { FileExtensions } from 'shared/models/File';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
  ICommunicationById,
} from 'shared/utils/redux/communication';
import { URL } from 'shared/utils/types';
import { IDatasetVersion } from 'shared/models/DatasetVersion';

export interface IArtifactManagerState {
  data: {
    url: URL | null;
    preview: string | null;
    datasetVersions: Record<string, IDatasetVersion | undefined>;
  };
  communications: {
    loadingArtifactUrl: ICommunication;
    downloadingArtifact: ICommunicationById;
    loadingArtifactPreview: ICommunication;
    loadingDatasetVersions: ICommunicationById;
    deletingArtifact: ICommunicationById;
  };
}

export type EntityType = Extract<
  Common.EntityType,
  'project' | 'experiment' | 'experimentRun' | 'release'
>;

export const ArtifactPreviewFileExtensions: Pick<
  typeof FileExtensions,
  'jpeg' | 'png' | 'gif' | 'json' | 'text' | 'txt'
> = {
  gif: 'gif',
  jpeg: 'jpeg',
  png: 'png',
  json: 'json',
  text: 'text',
  txt: 'txt',
} as const;
export type ArtifactPreviewFileExtension = typeof ArtifactPreviewFileExtensions[keyof (typeof ArtifactPreviewFileExtensions)];

export const loadArtifactUrlActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@artifactManager/LOAD_ARTIFACT_URL_REQUEST',
  SUCCESS: '@@artifactManager/LOAD_ARTIFACT_URL_SUCСESS',
  FAILURE: '@@artifactManager/LOAD_ARTIFACT_URL_FAILURE',
});
export type ILoadArtifactUrlActions = MakeCommunicationActions<
  typeof loadArtifactUrlActionTypes,
  {
    request: { key: string };
    success: { key: string; url: URL };
  }
>;

export const downloadArtifactActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@artifactManager/DOWNLOAD_ARTIFACT_REQUEST',
  SUCCESS: '@@artifactManager/DOWNLOAD_ARTIFACT_SUCСESS',
  FAILURE: '@@artifactManager/DOWNLOAD_ARTIFACT_FAILURE',
});
export type IDownloadArtifactActions = MakeCommunicationActions<
  typeof downloadArtifactActionTypes,
  {
    request: { key: string; entityId: string };
    success: { key: string; entityId: string };
    failure: { key: string; entityId: string; error: AppError };
  }
>;
export const getDownloadArtifactsKey = ({
  key,
  entityId,
}: {
  key: string;
  entityId: string;
}) => `${entityId}-${key}`;

export enum resetActionType {
  RESET = '@@artifactManager/RESET',
}
export interface IResetAction {
  type: resetActionType.RESET;
}

export const loadArtifactPreviewActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@artifactManager/LOAD_ARTIFACT_PREVIEW_REQUEST',
  SUCCESS: '@@artifactManager/LOAD_ARTIFACT_PREVIEW_SUCСESS',
  FAILURE: '@@artifactManager/LOAD_ARTIFACT_PREVIEW_FAILURE',
});
export type ILoadArtifactPreviewActions = MakeCommunicationActions<
  typeof loadArtifactPreviewActionTypes,
  { request: { key: string }; success: { key: string; preview: string } }
>;

export const loadDatasetVersionActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@artifactManager/LOAD_DATASET_VERSION_REQUEST',
  SUCCESS: '@@artifactManager/LOAD_DATASET_VERSION_SUCСESS',
  FAILURE: '@@artifactManager/LOAD_DATASET_VERSION_FAILURE',
});
export type ILoadDatasetVersionActions = MakeCommunicationActions<
  typeof loadDatasetVersionActionTypes,
  {
    request: { datasetVersionId: string };
    success: { datasetVersionId: string; datasetVersion: IDatasetVersion };
    failure: { datasetVersionId: string; error: AppError };
  }
>;

export const deleteArtifactActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@artifactManager/DELETE_ARTIFACT_REQUEST',
  SUCCESS: '@@artifactManager/DELETE_ARTIFACT_SUCСESS',
  FAILURE: '@@artifactManager/DELETE_ARTIFACT_FAILURE',
});
export type IDeleteArtifactActions = MakeCommunicationActions<
  typeof deleteArtifactActionTypes,
  {
    request: { experimentRunId: string };
    success: { experimentRunId: string };
    failure: { experimentRunId: string; error: AppError };
  }
>;

export type FeatureAction =
  | ILoadArtifactUrlActions
  | IDownloadArtifactActions
  | ILoadArtifactPreviewActions
  | IResetAction
  | ILoadDatasetVersionActions
  | IDeleteArtifactActions;
