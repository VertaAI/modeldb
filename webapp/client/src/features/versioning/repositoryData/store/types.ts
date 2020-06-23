import { IExperimentRunInfo } from 'shared/models/ModelRecord';
import { IBlob } from 'shared/models/Versioning/Blob/Blob';
import { IFolder } from 'shared/models/Versioning/RepositoryData';

export type IBlobView = IBlob & { experimentRuns: IExperimentRunInfo[] };

export type ICommitComponentView = IBlobView | IFolder;
