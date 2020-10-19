import * as R from 'ramda';

import { SHA } from '../RepositoryData';
import { IElementDiff, IBlobDiff, IArrayDiff } from './Diff';

export interface IEnvironmentBlob {
  category: 'environment';
  data: {
    data: IPythonEnvironmentBlob | IDockerEnvironmentBlob;
    variables?: IEnvironmentVariablesBlob[];
    commandLine?: string[];
  };
}

export type IEnvironmentBlobDiff = IBlobDiff<
  IEnvironmentBlobDataDiff,
  'environment',
  'environment'
>;
export type IEnvironmentBlobDataDiff = {
  variables?: Array<IElementDiff<IEnvironmentVariablesBlob>>;
  commandLine?: IElementDiff<string[]>;
  data?: IPythonEnvironmentBlobDiff | IDockerEnvironmentBlobDiff;
};
export interface IPythonEnvironmentBlobDiff {
  type: 'python';

  data: {
    requirements?: IArrayDiff<IPythonRequirementEnvironment>;
    constraints?: IArrayDiff<IPythonRequirementEnvironment>;
    pythonVersion?: IElementDiff<IVersionEnvironmentBlob>;
  };
}

export interface IDockerEnvironmentBlobDiff {
  type: 'docker';
  data: IElementDiff<{
    repository: string;
    tag?: string;
    sha?: SHA;
  }>;
}

export interface IEnvironmentVariablesBlob {
  name: string;
  value: string;
}

export interface IDockerEnvironmentBlob {
  type: 'docker';
  data: {
    repository: string;
    tag?: string;
    sha?: SHA;
  };
}
export const makeDockerImage = ({
  repository,
  sha,
  tag,
}: IDockerEnvironmentBlob['data']): string => {
  return `${repository}${sha ? `@${sha}` : ''}${tag ? `:${tag}` : ''}`;
};

export interface IPythonEnvironmentBlob {
  type: 'python';

  data: {
    requirements?: IPythonRequirementEnvironment[];
    constraints?: IPythonRequirementEnvironment[];
    pythonVersion?: IVersionEnvironmentBlob;
  };
}

export interface IPythonRequirementEnvironment {
  library: string;
  constraint?: string; // == != and so on
  version?: IVersionEnvironmentBlob;
}

export interface IVersionEnvironmentBlob {
  major?: number;
  minor?: number;
  patch?: number;
  suffix?: string;
}
export const versionEnvironmentToString = ({
  major = 0,
  minor = 0,
  patch = 0,
  suffix,
}: IVersionEnvironmentBlob): string => {
  return [major, minor, patch, suffix].filter(R.complement(R.isNil)).join('.');
};
