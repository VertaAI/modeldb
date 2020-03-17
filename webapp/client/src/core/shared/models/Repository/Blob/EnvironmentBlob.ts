import * as R from 'ramda';

import { SHA } from '../RepositoryData';
import { GenericDiff } from './Diff';

export interface IEnvironmentBlob {
  category: 'environment';
  data: {
    data: IPythonEnvironmentBlob | IDockerEnvironmentBlob;
    variables?: IEnvironmentVariablesBlob[];
    commandLine?: string[];
  };
}

export type IEnvironmentBlobDiff = GenericDiff<
  IEnvironmentBlobDataDiff,
  'environment',
  'environment'
>;
export type IEnvironmentBlobDataDiff = {
  variables?: IEnvironmentVariablesBlob[];
  commandLine?: string[];
  data?: IPythonEnvironmentBlob | IDockerEnvironmentBlob;
};

export const checkCommonEnvironmentDataIsChanged = (
  dataDiffA: IEnvironmentBlobDataDiff | undefined,
  dataDiffB: IEnvironmentBlobDataDiff | undefined
) => {
  return (
    (dataDiffA && (dataDiffA.commandLine || dataDiffA.variables)) ||
    (dataDiffB && (dataDiffB.commandLine || dataDiffB.variables))
  );
};

export const safeMapPythonBlobDataDiff = <
  K extends keyof IPythonEnvironmentBlob['data'],
  T
>(
  blob: IEnvironmentBlobDataDiff | undefined,
  key: K,
  f: (d: Required<IPythonEnvironmentBlob['data']>[K]) => T
): T | null =>
  blob && blob.data && blob.data.type === 'python' && blob.data.data[key]
    ? f(blob.data.data[key] as any)
    : null;

export const safeMapDockerBlobDataDiff = <
  K extends keyof IDockerEnvironmentBlob['data'],
  T
>(
  blob: IEnvironmentBlobDataDiff | undefined,
  key: K,
  f: (d: Required<IDockerEnvironmentBlob['data']>[K]) => T
): T | null =>
  blob && blob.data && blob.data.type === 'docker' && blob.data.data[key]
    ? f(blob.data.data[key] as any)
    : null;

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
