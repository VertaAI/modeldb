import { IExperimentRunInfo } from 'core/shared/models/ModelRecord';
import { IBlob } from 'core/shared/models/Versioning/Blob/Blob';
import { IFolder } from 'core/shared/models/Versioning/RepositoryData';

export type IBlobView = IBlob & { experimentRuns: IExperimentRunInfo[] };

export type ICommitComponentView = IBlobView | IFolder;
